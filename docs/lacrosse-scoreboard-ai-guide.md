# Lacrosse Scoreboard 功能与字典指引

本文件是本仓库的业务事实来源。开发前必须先核对这里的功能、状态和字典约定；修改相关行为时必须同步更新本文件。

## 1. 项目边界

- `La-Scoreboard/`：计分台前端，当前使用 Vite 和原生 JavaScript，核心规则模型位于 `src/app.js`，测试位于 `src/tests.js`。
- `la-scoreboard-vue/`：Vue 3 迁移版计分台。Vue 管理应用入口、生命周期和响应式比赛状态；规则模板、比赛初始模型、计时推进、动作策略、事件生成与校验、比赛结果核对、缓存恢复、比赛记录查询/导出及全部比赛状态动作位于 `src/core`，后端详情转换位于 `src/adapters/matchDetailAdapter.js`，运行引擎位于 `src/engine/scoreboardEngine.js`。旧版 `src/legacy` 兼容层已经删除，代码必须按职责直接导入核心模块。统一请求层位于 `src/api/request.js`，计分业务接口位于 `src/api/scoreboard.js`。
- `edus-core/`：Spring Boot / MyBatis-Plus 后端模块。
- `db/scoreboard.sql`：计分器平台表结构，兼容 MySQL 5.6。
- `db/migrations/`：已有数据库的一次性升级脚本；新建数据库只执行最新 `scoreboard.sql`。
- `db/archive_user.sql`：外部信息管理平台的参考结构，维护球队、球员、教练、裁判、人员和数据字典。
- 两个平台之间只保存外部业务 ID，不建立跨库外键。计分器自身表之间可以建立外键。

## 2. 核心业务流程

### 2.1 创建比赛

1. 用户通过下拉框选择组别。组别选项必须由 `GET /scoreboard/dictionaries/age-groups` 查询启用的 `sys_dict_item.type_code = 'team_age_group'` 数据；页面展示 `item_label`，提交和保存 `item_value`，不得提供自由文本输入。
2. 页面按组别展示 `scoreboard_group_rules` 中启用的规则模板。同一组别可以有多个模板，也可以不选择模板直接填写自定义规则。
3. 选择模板后仍可修改本场最终规则；自定义规则可选择“仅用于本场”，也可填写模板名称后“另存为模板”。
4. 在同一事务中创建：
   - `scoreboard_matches`：始终保存 `age_group` 字典业务值；选用或新建模板时保存 `group_rule_id`，仅用于本场的自定义规则不保存模板 ID；
   - `scoreboard_match_rules`：始终保存本场最终规则快照；与引用模板一致时 `is_customized = 0`，仅用于本场或修改过所选模板时为 `1`；
   - `scoreboard_match_state`：按最终规则初始化比赛钟、进攻钟、暂停次数和交替球权；
   - 本场球队、球员名单等需要同时保存的数据。
5. 修改本场规则不得反向更新原模板。另存为模板必须新增 `scoreboard_group_rules` 记录；以后修改模板也不得影响已创建比赛。

### 2.2 球队与本场名单

- 主客队来源于外部 `team_info.id`，比赛表同时保存队名和队服颜色快照。
- 本场球员使用 `scoreboard_match_roster.id` 作为比赛内关联 ID。
- `scoreboard_match_roster.team_player_id` 对应外部 `team_player.id`，允许先录姓名和号码、后续回填。
- 进球、助攻、犯规和代罚球员优先关联本场名单 ID，不直接关联 `team_player.id`，以保证历史比赛稳定。
- 计分和判罚时优先从本场名单匹配进球、助攻、被判罚和服罚球员；本场名单暂为空或尚未回填时，允许直接填写球号或姓名，并分别保存为事件的 `subject_name`、`assist_name`、`served_by_name` 快照，不得强制要求人员必须来自本场名单。名单完善后可回填对应名单 ID。
- 同一场、同一球队的球衣号码不可重复。

### 2.3 教练、裁判和记分员

- 教练属于球队，不与裁判混为同一业务关系，保存于 `scoreboard_match_coaches`。
- 教练通过 `team_side` 区分主队和客队，通过 `team_coach_rel_id` 对应外部 `team_coach_rel.id`；允许比赛创建后补录。
- 裁判和记分计时员属于本场执裁安排，保存于 `scoreboard_match_officials`，通过 `person_id` 对应外部 `person_profile.id`。
- 姓名必须保存快照，避免外部人员资料变更影响历史记录。
- 当前不在线处理签字，不设计电子签名字段。纸质记录表导出后线下签署。

### 2.4 实时计分与持久化

- 前端负责即时显示和本地平滑倒计时；点击操作后立即更新界面，再异步请求后端。
- 后端保存权威状态并校验状态转换。数据库只在开始、暂停、继续、进球、犯规、换节等操作节点更新，不每秒写库。
- `scoreboard_match_state.state_version` 用于乐观锁，防止多端或重复请求覆盖新状态。
- 修改比赛的接口必须携带当前 `state_version`，并在服务层处理重复点击、超时重试和乱序请求；这些属于接口与事务要求，当前不为此单独建设业务表。
- `timer_started_at` 是本次计时启动锚点；剩余时间以整数秒保存。前端内部允许使用毫秒保持动画流畅，但 SQL、接口业务单位必须明确为分钟或秒。
- 状态保存请求是完整快照；`clear_remaining_seconds`、暂停上下文、球权、交替球权和待确认进球等可空字段传入 `null` 时表示明确清除，后端不得因忽略空值而保留旧状态。
- `scoreboard_matches.started_at` 保存首次鸣哨时间，`finished_at` 保存确认比赛结束时间。
- `scoreboard_match_state.clear_remaining_seconds` 必须保留三态语义：`NULL` 表示推进提示未启动或已取消，`0` 表示已经到时并等待裁判确认，大于 `0` 表示当前剩余秒数。不得用默认值 `0` 代替 `NULL`。

### 2.5 比赛事件与审计

- 进球、球队暂停、判罚、球权、更正和系统节点统一进入 `scoreboard_match_events` 时间线。
- 所有比赛事件统一保存 `event_title` 简要标题和 `event_text` 详细说明；`rule_reference` 只保存事件发生时实际采用的规则条款或本场规则来源，没有明确依据时为空，不得用事件分类名称代替规则依据。
- 球队暂停等具有配置时长的事件同时保存 `duration_seconds` 时长快照；标题和详细说明都必须展示该时长。节间、中场和加时休息等系统事件也必须在标题与详细说明中保留实际配置时长；裁判暂停等无固定时长的操作不填写该字段。
- 首场首次鸣哨保存为 `MATCH_START`，后续各节首次鸣哨保存为 `PERIOD_START`，节内复赛和独立点击的裁判停表保存为 `SYSTEM`；比赛钟归零保存为 `PERIOD_END`，确认比赛结束保存为 `MATCH_END`，普通球权和交替球权判给球队保存为 `POSSESSION`。不得把这些可结构化的比赛节点全部降级为 `SYSTEM`。
- 判罚引起的停表属于同一次判罚业务动作，只新增一条 `PENALTY` 事件，并在该事件详细说明中记录“计时停止于 mm:ss”；不得先新增“裁判暂停”系统事件再新增判罚事件。权威停表点仍保存为该事件的 `period_no` 和 `remaining_seconds`，说明文字只是展示快照。打开判罚弹窗需要立即停表时只保存运行状态，确认判罚后再写入事件。
- 每节首次鸣哨前必须确认实际开场球权；准备阶段选择只更新待开赛状态、不单独生成事件，鸣哨后由 `MATCH_START` 或 `PERIOD_START.team_side` 保存并展示开场球权。新建或重置比赛时必须按本场规则配置初始化：`alternate_first=home|away` 分别将主队或客队写入本场初始球权和下一次交替球权，`manual` 则不预设球队并等待裁判在页面选择。计分员可在首次鸣哨前按裁判决定更改初始球权，只有鸣哨后该球权才成为正式开场记录；`alternate_side` 始终表示下一次交替球权。
- 事件必须在动作发生时冻结节次和比赛钟剩余时间，不得在异步提交时使用已经推进的当前时间；`scoreboard_matches.started_at` 采用首次 `MATCH_START.occurred_at`，确保比赛开始时间只有一个事实来源。
- 球队暂停条件确认可以原子更新本队球权、前场和死球状态，但这些是暂停动作内部状态，不单独生成 `POSSESSION` 或“进入前场”事件；一次暂停操作只新增一条 `TEAM_TIMEOUT` 记录，条件保存在该记录中。
- 球队暂停辅助钟归零时必须自动执行统一的暂停结束动作，不要求记分员再次点击；提前手动结束与自动到时结束使用同一状态转换。结束时新增一条携带原暂停球队 `team_side` 的 `SYSTEM` 记录，标题和说明均以“某某球队暂停结束”开头，不再额外写“暂停时间到”记录。
- `sequence_no` 是每场比赛内从 1 递增的稳定顺序号；不要依赖全局自增 `id`、倒计时或秒级时间排序。
- `occurred_at` 是事件实际发生时间，`created_at` 是写库时间；补录时两者可能不同。
- 更正和作废必须保留原记录：原记录标记为 `REPLACED` 或 `CANCELLED`，新记录通过 `related_event_id` 指向原记录并保存依据。
- 总比分保存在比赛表作为查询摘要，同时必须与有效进球及裁判核定结果保持一致。
- 分节比分从有效进球按 `period_no` 汇总，不因纸质表存在分节格子就重复建表。
- 判罚的原始内容保存在 `scoreboard_match_events`；有罚时判罚的可变剩余时间和执行状态保存在 `scoreboard_match_penalties`，不得持续覆盖原始事件。
- 罚时不需要每秒写库。操作节点保存整数剩余秒数；处于 `ACTIVE` 状态时与比赛钟共用 `scoreboard_match_state.timer_started_at` 计算实时剩余时间。
- 正常运行的罚时倒计时归零时自动改为 `RELEASED`，立即从罚时区移除，并新增一条“罚时结束，已自动释放”的系统记录；不再要求记分员点击确认释放。`EXPIRED` 仅用于兼容旧数据或人工将剩余罚时更正为零后的待确认状态，仍保留手动释放入口。
- 重大犯规次数和被罚退球员名单不单独落表，必须由当前有效的 `PENALTY` 事件重建，避免判罚更正或作废后出现两份结果。球员和守门员优先按 `roster_id` 累计；名单尚未关联时暂按球队和人员文字快照累计，回填后以名单 ID 为准。球队官员判罚不计入球员重大犯规。
- 更正判罚时，原事件改为 `REPLACED`，新增的有效事件仍使用 `PENALTY` 并通过 `related_event_id` 指向原事件；作废时原事件改为 `CANCELLED`，另写 `CORRECTION` 审计事件。累计时只读取 `event_type = PENALTY` 且 `event_status = VALID` 的记录。

## 3. 主要功能

### 3.1 比赛与规则

- 新建比赛、修改未开赛比赛信息、选择已有规则模板、创建本场自定义规则。
- 自定义规则既可仅用于当前比赛，也可命名后保存为同组别下可复用的新模板。
- 每队人数、常规节数、每节时长、中场位置、进攻钟、推进提示和每球得分。
- 节间、中场、加时休息和球队暂停时长/次数。
- 普通阶段、末段和加时阶段的停表规则。
- 加时策略、金球规则、判罚时长、守门员代罚和交替球权规则。

### 3.2 计时台

- 比赛钟开始、暂停和人工更正。
- 进攻钟重置、末段关闭、射偏不自动重置。
- 推进提示、前场状态、活球/死球状态和当前球权。
- 节结束、节间/中场休息、下一节准备、加时和比赛结束。
- 球队暂停及暂停条件校验；暂停倒计时归零自动结束，或由记分员提前结束，并恢复原阶段。
- 页面刷新或多端同步时根据后端状态和计时锚点恢复。

### 3.3 比赛记录

- 进球：球队、得分球员、助攻球员、分值、节次和剩余时间。
- 点击“进球 +1”只进入待确认状态并暂停计时，打开录入弹窗和取消未确认录入都不生成比赛事件；只有确认成功后才写入一条 `GOAL` 记录。
- 补录进球与进球球员信息补充。
- 判罚：犯规球队、犯规主体、牌型、罚时、代罚球员、犯规项、就座和释放状态；判罚停表写在同一条判罚详情中，不额外生成裁判暂停记录。
- 球队暂停：所属球队、次数归属和裁判确认条件。
- 普通球权和交替球权记录。
- 判罚、暂停、比分、比赛钟和进攻钟更正；所有更正需要依据。
- 比赛记录导出，导出布局不决定数据库表结构。

## 4. 前端规则模板

当前 `La-Scoreboard/src/app.js` 中的模板代码：

| 值 | 含义 | 主要默认值 |
|---|---|---|
| `sll-u8` | U8 | 4 人、2 节、每节 8 分钟、无进攻钟 |
| `sll-u10` | U10 | 4 人、2 节、每节 10 分钟、无进攻钟 |
| `sll-u12` | U12 | 4 人、2 节、每节 10 分钟、无进攻钟 |
| `sll-sixes` | 六人制 | 6 人、4 节、每节 8 分钟、30 秒进攻钟 |
| `custom` | 自定义 | 以模板为基础修改本场规则 |

后端落库时不依赖前端模板名称作为唯一事实，最终规则以 `scoreboard_match_rules` 为准。

## 5. 数据字典与固定代码

### 5.1 外部数据字典规则

- `type_code`：定位一类字典，例如 `team_age_group`。
- `item_value`：具体业务值，保存到业务配置中。
- `item_label`：展示文本，不作为业务关联条件。
- 不使用 `sys_dict_item.id` 做跨平台业务值，因为不同环境的自增 ID 可能不同。

当前外部字典：

| type_code | 用途 | 保存位置 |
|---|---|---|
| `team_age_group` | 球队/比赛组别 | `scoreboard_group_rules.age_group`、`scoreboard_matches.age_group` 保存 `item_value` |
| `coach_team_role` | 球队教练类型 | `scoreboard_match_coaches.coach_type` 保存 `item_value` |

组别已经包含实际分组含义时，不额外增加 `gender` 字段。

### 5.2 比赛及状态代码

| 字段 | 可用值 |
|---|---|
| `stage` | `league` 常规赛；`knockout` 淘汰/名次赛 |
| `status` | `READY` 待开始；`ONGOING` 进行中；`FINISHED` 已结束 |
| `phase` | `READY` 待鸣哨；`PERIOD` 比赛节；`PERIOD_END` 本节结束；`BREAK` 节间/中场；`TIMEOUT` 球队暂停；`FINISHED` 比赛结束 |
| `team_side` | `home` 主队；`away` 客队 |
| `ball_state` | `dead` 死球；`live` 活球 |
| `auxiliary_type` | `BREAK` 节间；`HALFTIME` 中场；`OVERTIME_BREAK` 加时休息；`TIMEOUT` 球队暂停 |

### 5.3 规则代码

| 字段 | 可用值 |
|---|---|
| `overtime_policy` | `always` 始终加时；`knockout` 仅淘汰赛加时；`none` 不加时 |
| `alternate_first` | `manual` 手动指定；`home` 主队；`away` 客队 |
| 布尔规则字段 | `0` 否；`1` 是 |

### 5.4 事件代码

| 字段 | 可用值 |
|---|---|
| `event_type` | `MATCH_START`、`PERIOD_START`、`PERIOD_END`、`MATCH_END`、`GOAL`、`TEAM_TIMEOUT`、`PENALTY`、`POSSESSION`、`CORRECTION`、`SYSTEM` |
| `event_status` | `VALID` 有效；`CANCELLED` 已作废；`REPLACED` 已被更正 |
| `timeout_bucket` | `FIRST_HALF` 上半场；`SECOND_HALF` 下半场；`OVERTIME` 加时 |
| `timeout_condition` | `BEFORE_START` 历史赛前记录兼容值（不再允许新增）；`HALFTIME` 中场；`FRONTCOURT` 前场控球；`DEAD_BALL` 死球 |
| `penalty_subject` | `player` 球员；`goalie` 守门员；`official` 球队官员 |
| `penalty_type` | `turnover` 仅换球权；`minor` 绿牌；`major` 黄牌；`expulsion` 红牌 |
| `penalty_state` | `WAITING` 待就座；`SEATED` 已就座待复赛；`ACTIVE` 执行中；`EXPIRED` 已到时待释放；`RELEASED` 已释放 |

### 5.5 比赛人员代码

| 字段 | 可用值 |
|---|---|
| `official_role` | `REFEREE` 主裁判；`ASSISTANT_REFEREE` 助理裁判；`BENCH_OFFICIAL` 替补席裁判；`SCOREKEEPER` 记分计时员 |
| `coach_type` | 使用外部字典 `coach_team_role.item_value`，当前参考值为 `HEAD`、`OTHER` |

## 6. 数据库约束

- `db/scoreboard.sql` 必须兼容 MySQL 5.6，不使用 `JSON`、`CHECK`、生成列或 MySQL 8 专属语法。
- 字段和注释使用中文说明；枚举备注采用“中文用途：代码=含义”的格式。
- `scoreboard_group_rules` 以 `age_group + template_name` 唯一，同一组别允许多个模板。
- `scoreboard_matches.age_group` 保存 `team_age_group.item_value`；`group_rule_id` 仅在选用或新建模板时关联 `scoreboard_group_rules.id`，纯本场自定义时允许为空。
- `scoreboard_match_rules.match_id`、`scoreboard_match_state.match_id` 同时是主键，确保一场比赛各只有一条规则快照和运行状态。
- `scoreboard_match_penalties.penalty_event_id` 唯一，一条有罚时判罚事件只能对应一条罚时运行记录；仅换球权判罚不创建罚时运行记录。
- `PENALTY` 的犯规主体为球员或守门员时优先填写 `roster_id`；名单尚未录入时允许使用 `subject_name` 和 `served_by_name` 保存被判罚与服罚人员文字快照。同名人员只有在回填名单 ID 后才能稳定区分身份。
- 外部的 `team_info.id`、`team_player.id`、`team_coach_rel.id`、`person_profile.id`、`sys_user.id` 只记录 ID，不在计分器数据库建立外键。
- 不为 Excel 的固定行数、合并单元格、签字位置等展示结构建字段或表。

## 7. 状态转换要点

| 操作 | 比赛状态/阶段 | 关键更新 |
|---|---|---|
| 创建比赛 | `READY / READY` | 初始化规则快照、比赛钟、暂停额度和交替球权 |
| 首次鸣哨 | `ONGOING / PERIOD` | 写 `started_at`，插入 `MATCH_START`，启动比赛钟 |
| 暂停比赛钟 | `ONGOING / PERIOD` | 保存剩余秒数，`main_running = 0` |
| 球队暂停 | `ONGOING / TIMEOUT` | 扣减对应额度，启动辅助暂停钟，保存原阶段上下文 |
| 记录有罚时判罚 | `ONGOING / PERIOD` | 首次鸣哨后方可写入判罚事件并创建 `WAITING` 罚时，所有计时保持停止 |
| 确认服罚球员就座 | `ONGOING / READY或PERIOD` | 罚时改为 `SEATED`，等待鸣哨复赛 |
| 鸣哨复赛 | `ONGOING / PERIOD` | 将全部 `SEATED` 罚时改为 `ACTIVE`，与比赛钟同步计时 |
| 罚时到时或释放 | `ONGOING / PERIOD` | 正常倒计时到零自动改为 `RELEASED` 并记录自动释放；旧数据或人工置零形成的 `EXPIRED` 可手动释放 |
| 更正判罚 | 保持当前阶段并停表 | 原判罚改为 `REPLACED`，新增有效 `PENALTY`，同步罚时运行记录并重算重大犯规及罚退名单 |
| 作废判罚 | 保持当前阶段并停表 | 原判罚改为 `CANCELLED`，移除对应罚时运行记录并重算重大犯规及罚退名单 |
| 本节结束 | `ONGOING / PERIOD_END` | 停止相关时钟，插入 `PERIOD_END` |
| 进入休息 | `ONGOING / BREAK` | 按普通节间、中场或加时规则初始化辅助钟 |
| 准备下一节 | `ONGOING / READY` | 节次加一，初始化下一节比赛钟和相关状态 |
| 比赛结束 | `FINISHED / FINISHED` | 写 `finished_at`，插入 `MATCH_END` |

赛后比分更正可以将 `FINISHED` 重新置为 `PERIOD_END`，金球比赛恢复平局且仍有剩余时间时也可以回到 `PERIOD`；此时比赛状态恢复为 `ONGOING` 并清空旧 `finished_at`，重新确认结束时再写入新的结束时间。

状态切换、比分更新、事件写入和状态版本更新必须在同一后端事务中完成。

`scoreboard_match_state` 必须保存刷新或重连后恢复计分台所需的完整运行上下文：复赛是否重置进攻钟、金球及冲突状态、球队暂停前的阶段与辅助计时状态，以及待确认进球草稿。待确认进球球员和助攻使用 `scoreboard_match_roster.id`，不保存自由文本姓名。球队暂停结束时按 `timeout_return_*` 字段恢复原阶段；进球确认或取消后必须清空全部 `pending_goal_*` 字段。

## 8. 后端实现约定

- 计分业务统一放在 `edus-core` 的 `com.edus.score` 包内，按 `controller`、`service`、`mapper`、`entity`、`dto` 分层，不移动或混入现有 `auth`、`example` 业务。
- 比赛创建接口为 `POST /scoreboard/matches`。服务层在一个事务中创建比赛、保存本场最终规则快照、按需新增规则模板、初始化运行状态，并保存本场名单、教练和执裁人员。
- 比赛详情接口为 `GET /scoreboard/matches/{matchId}`，返回比赛、规则快照、运行状态、名单、事件、罚时、教练和执裁人员，供计分页面刷新或断线恢复。比赛中的 `group_rule_id` 只表示创建时选择的规则关联，详情接口不要求额外返回完整组别规则。
- 清空比赛记录接口为 `POST /scoreboard/matches/{matchId}/records/clear`，请求必须携带 `expectedVersion`。该操作不删除赛程，保留比赛基础信息、本场规则快照、名单、教练和执裁人员，将比赛恢复为 `READY`、比分归零、开始和结束时间清空、运行状态按本场规则重新初始化，并删除该场比赛的事件和罚时记录；全部修改必须在同一事务中完成。该操作会删除历史记录，页面必须二次确认并明确展示保留与清空范围。
- 实时状态通过 `PUT /scoreboard/matches/{matchId}/state` 保存，请求必须携带 `expectedVersion`；比分、比赛状态、运行状态、本次操作产生的 `events` 事件列表、原事件的 `eventStatuses` 状态变更以及现有 `penalties` 罚时状态在同一事务内完成。判罚事件变为 `CANCELLED` 或 `REPLACED` 时同步移除其旧罚时运行记录；更正判罚以新的有效 `PENALTY` 事件创建新罚时。响应返回最新状态、本次新增或变更的事件和本次新增或更新的罚时记录，供前端回填数据库 ID 和下一版 `stateVersion`。
- 独立新增事件和修改罚时状态也必须携带 `expectedVersion`，响应返回新的 `stateVersion`，客户端后续操作必须使用新版本号。
- 后端校验阶段转换、比赛状态、计时运行关系、名单归属、进球球队、判罚主体和服罚球员；前端即时显示不能替代这些服务端校验。
- 事件请求只提交结构化业务字段，不接收前端生成的 `event_text` 和 `rule_reference`。事件类型统一使用 `MatchEventType`，中文说明和默认规则依据由 `MatchEventContentUtil` 集中生成。
- `event_text` 和 `rule_reference` 是事件发生时的展示快照；修改文案模板只影响以后新增的事件，不重写历史比赛记录。`SYSTEM` 事件使用请求中的 `description` 生成说明，其他事件不得由前端自定义文案。
- 后端事件代码统一由 `MatchEventType` enum 管理；Vue 事件代码、展示分类和开节语义统一由 `src/core/eventTypes.js` 的冻结对象管理。新增或删除事件类型时必须同步修改两处定义、事件内容生成、SQL 注释、本节代码表和对应测试，不得在业务文件中新增散落的事件代码清单。
- 组别规则、本场规则和状态字段的 Bean 复制、字符串与集合判断优先使用 Hutool；仅在没有合适工具且属于通用能力时，才允许在 `util` 中补充自定义工具类。
- 当前不实现记分员登录码。后续增加时，应独立设计比赛授权与有效期，不把登录码明文直接放入比赛基础表。

## 8.1 Vue 前端迁移约定

- Vue 迁移不得重新实现或删减未经验证的比赛规则；计分规则统一保留在 `src/core` 并由独立测试覆盖，页面按计时台、球队区、罚时区、事件记录和弹窗进行组件化。
- Vue 当前比赛状态统一由 Pinia 的 `src/stores/scoreboardStore.js` 持有，并在 `main.js` 注册唯一 Pinia 实例。计分引擎原地修改对象后必须调用 store action `notify()`；`notify()` 必须替换比赛根对象引用，确保接收 `match` 属性的时钟等子组件持续重绘，不得只触发浅层 ref。新 Vue 组件通过 `useScoreboardStore()` 使用同一比赛状态，不得另建重复状态。Store 同时保存远程比赛 ID、后端原始详情、`stateVersion`、最后同步时间及同步状态；同步状态使用 `local`、`loading`、`syncing`、`synced`、`offline`、`conflict`、`error`。版本冲突后禁止继续提交，必须重新加载比赛详情。后端原始详情必须经过 `src/adapters/matchDetailAdapter.js` 转换字段、代码值、名单引用及秒/毫秒单位，并按 `timer_started_at` 推进正在运行的时钟后，才能替换前端计分对象。页面地址使用 `?matchId={比赛ID}` 时加载指定服务器比赛；未指定时查询比赛列表并加载最近一场。没有任何服务器比赛时使用 `createEmptyMatch()` 展示完整计分台，比赛钟、进攻钟、推进钟、比分和暂停次数均为 0；占位计时区只显示中央比赛钟 `00:00`，不显示尚无规则依据的进攻钟和推进钟。“新建比赛”、主题切换、全屏及“计时台／本场规则／比赛记录”页签保持可用，其余比赛业务控件禁用；页面交互层和动作分发层都必须阻止占位比赛操作，不得展示本地缓存中的旧比赛。弹窗、页签等仅属于单个页面的界面状态继续保留在组件内。
- 组别是后端数据字典值，新建比赛时必须从字典接口提供的选项中选择，不得从规则模板反推或由前端自行维护；详情接口未提供预设名称时按自定义规则展示，本场计时和判罚参数仍以规则快照为准。
- 迁移期间的浏览器比赛缓存底层序列化统一由 `src/storage/matchStorage.js` 处理，初始化、恢复、保存和跨标签页写入锁统一由 `src/storage/scoreboardPersistence.js` 管理；规则引擎和 Vue 组件不得直接访问 `localStorage`、拼接缓存键或自行序列化比赛。规则快照校验按字段和值比较，不得依赖对象字段顺序。缓存确实损坏或不兼容时必须由持久化层拦截恢复异常并锁定缓存写入，不在比赛页面展示缓存读写失败提示，也不得静默覆盖原缓存；跨页面修改仍需提示以防覆盖。浏览器缓存只是断线保护，接入后端后不能作为权威数据源。
- `ScoreboardEngine` 必须由 Vue 组件负责创建；计时轮询、跨标签页缓存冲突、页面隐藏或离开时保存以及蜂鸣音统一由 `src/composables/useScoreboardRuntime.js` 管理，组件卸载时必须停止运行时并清理全部 `window`、`document` 监听，避免热更新或页面切换后重复计时。计分引擎不得直接访问 `window`、`document`，也不得覆盖或清空 Vue 管理的根节点。
- 页面挂载后必须立即初始化 `ScoreboardEngine` 并启动计时轮询，不得等待组别字典、规则模板或远程比赛请求完成；这些网络请求失败或超时时，本地鸣哨与倒计时仍须正常工作。
- 动作代码、弹窗准备和表单业务类型的调度统一由 `src/composables/useScoreboardController.js` 处理；控制器只能组合核心模块已有的规则校验和状态转换，不得复制另一套比赛规则。规则模板及字段校验位于 `src/core/rules.js`，比赛初始状态由 `src/core/matchModel.js` 创建，节次判断、时钟格式化、停表条件和基于实际耗时的倒计时推进位于 `src/core/timing.js`，按钮可用性、规则可编辑条件和比赛更正参数解析位于 `src/core/actionPolicy.js`，事件编号与记录快照、进球内容、判罚数据校验、纪律累计、暂停额度归属及更正记录查找位于 `src/core/matchEvents.js`，开赛复赛、裁判停表、球权、进攻钟、推进提示和交替球权位于 `src/core/fieldActions.js`，本节确认、进入休息和准备下一节位于 `src/core/periodActions.js`，比赛钟、进攻钟、比分、单条罚时、赛后比分及金球冲突更正位于 `src/core/correctionActions.js`，待确认进球、即时进球、补录进球及进球信息补充的状态动作位于 `src/core/goalActions.js`，球队暂停条件确认、开始、结束和记录更正位于 `src/core/timeoutActions.js`，判罚新增、更正、作废、罚时就座和释放位于 `src/core/penaltyActions.js`，比赛名称、日期和主客队名称修改位于 `src/core/matchInfoActions.js`，动作入口、前置状态约束、事务式副本提交和单步撤销位于 `src/core/scoreboardActions.js`，有效比赛记录合并、犯规统计和 CSV 导出位于 `src/core/matchRecords.js`，加时及金球结果重算位于 `src/core/matchOutcome.js`，缓存结构校验与安全停表恢复位于 `src/core/matchRecovery.js`。状态仓库连接、缓存读取后的业务恢复、计时提交和状态通知统一由 `src/engine/scoreboardEngine.js` 处理。
- 比赛钟、进攻钟、推进辅助钟、节间/暂停钟和罚时的毫秒扣减、显示格式及业务推进统一由 `src/core/timing.js` 处理。计时推进期间必须通知 Pinia 刷新页面，不能只在倒计时到期产生事件时刷新。
- 主比赛钟、进攻钟、推进辅助钟、节间/暂停钟及换节入口由 `components/scoreboard/MatchClock.vue` 渲染；状态转换仍统一调用既有计分引擎，迁移期间不得在组件中复制另一套操作规则。
- 鸣哨、裁判暂停、罚时待办、撤销和比赛更正入口由 `components/scoreboard/MatchClockControls.vue` 渲染；罚时筛选和按钮可用条件集中在同目录的 `matchClockControls.js`，组件只负责展示并向既有引擎派发操作。新建比赛尚未首次鸣哨时“裁判暂停”必须禁用；只有本节已经开始且比赛钟正在运行时才能暂停，已经停表时也不得重复暂停。
- 主客队比分、进球、球队暂停、判罚入口和罚退展示统一由 `components/scoreboard/TeamPanel.vue` 渲染，通过 `team=home|away` 复用；进球、暂停和判罚按钮条件集中在同目录的 `teamPanel.js`。比赛首次鸣哨前以及当前已经处于球队暂停时，主客队“暂停”按钮都必须禁用，不得重复申请暂停；比赛首次鸣哨前，“记录判罚”区域的全部判罚按钮也必须禁用。
- 普通球权、交替发球、一般死球、进攻钟重置和推进状态由 `components/scoreboard/FieldControls.vue` 渲染；组件只提交既有动作代码，状态转换和裁判条件仍由计分引擎统一校验。
- 当前罚时列表由 `components/scoreboard/PenaltyList.vue` 渲染，展示牌型、犯规原因、代罚、罚退、剩余时间和执行状态；筛选及展示文案集中在同目录的 `penaltyList.js`，就座与释放仍由计分引擎处理。
- 计时台最近记录和完整比赛记录统一由 `components/scoreboard/MatchRecords.vue` 渲染，组件负责分类筛选展示、滚动位置和详情展开状态；事件更正、作废和进球信息补充仍派发既有动作并保留原记录。每条记录的操作按钮必须紧跟在事件标题后，与标题处于同一行，不得放在详细说明、规则依据或更正依据之后。
- 比赛记录的“相关队伍／球员”按业务主体展示：鸣哨、裁判暂停、死球、普通或交替球权判定、前后场与进攻钟操作、换节休息、比赛结束及全局更正显示“全场”；球队暂停及暂停结束显示对应球队；进球、判罚和罚时执行显示对应球队与人员；球队官员判罚明确标注“球队官员”；进球、判罚和暂停更正继承原记录或更正后的关联主体。关联对象必须来自事件结构化字段，不得从说明文本猜测。
- 比赛标题、信息入口、服务器比赛的“清空比赛记录”入口、主题、导出和全屏操作由 `components/scoreboard/MatchHeader.vue` 渲染；清空入口在服务器比赛加载后始终显示在页面顶部，并使用 `ClearMatchRecordsDialog.vue` 明确说明赛程不会删除、展示保留与清空范围并要求确认。页面摘要和计时台/规则/记录切换由 `MatchNavigation.vue` 渲染，当前页面状态由 Vue 直接持有并切换内容。服务器的读取中、同步中、已同步、离线、冲突和失败状态统一在页面摘要末尾紧凑显示“状态 #比赛ID”，不得在计时区上方另占一行，避免操作时页面上下跳动；失败详细原因通过该状态文字的提示属性保留。
- 本场规则摘要、修改入口、得分判罚说明和裁判确认事项由 `components/scoreboard/MatchRules.vue` 渲染；显示值直接取本场规则快照并按业务单位转换，不回写规则模板。
- Vue 弹窗统一复用 `components/common/BaseDialog.vue`：标题和关闭按钮位于固定顶部，业务表单放在可滚动中间区域，取消与确认按钮位于固定底部；具体弹窗不得重复实现外壳。
- 新建比赛和修改本场规则由 `components/scoreboard/MatchSetupDialog.vue` 渲染，规则预设只用于填充表单，提交的最终字段仍形成本场规则快照。
- 修改比赛名称、日期和球队名称由 `MatchInfoDialog.vue` 处理，只修改比赛基础信息，不交换主客身份或改动比分、规则和计时状态；CSV/JSON 导出由 `MatchExportDialog.vue` 提供，两个弹窗均复用公共弹窗外壳。
- 即时进球和补录进球统一由 `GoalEntryDialog.vue` 处理，球队切换、球员与助攻、补录节次时间及比分处理仍提交到同一计分核心；已记分进球的球员与助攻修改由 `GoalDetailsDialog.vue` 处理，不改变比分和计时。两个弹窗均复用公共弹窗外壳。
- 点击“进球 +1”进入即时进球录入时必须立即调用 `timing.js` 的公共 `freeze()` 停止比赛计时，并转为死球；确认进球后保持停表，等待裁判鸣哨复赛。即时进球弹窗的输入草稿只在弹窗模式或待确认进球编号变化时重建，不得因比赛倒计时刷新而清空姓名或号码。关闭未确认的即时进球弹窗视为取消误操作，应清除待确认进球并恢复打开弹窗前的计时、死活球和推进状态，使进球入口可以再次使用。
- 进球、判罚、暂停和更正等业务表单提交后必须等待对应状态与事件保存请求成功才关闭弹窗；服务器返回失败时弹窗保留并展示错误，不得仅更新本地页面后把后台同步失败伪装成提交成功。
- 状态保存成功后，前端必须立即采用响应中的事件文案、规则依据、球队、球员和状态快照更新本地时间线，确保保存后与刷新后展示一致；前端提交前生成的临时说明不得继续覆盖数据库权威快照。
- 球队暂停确认由 `TeamTimeoutDialog.vue` 处理，按当前阶段只展示可提交的中场、前场控球或死球后球权条件；比赛开始前不提供暂停条件。条件按钮固定在公共弹窗底部，状态转换和暂停资格仍由计分核心最终校验。
- 新增判罚由 `PenaltyDialog.vue` 处理，入口指定的球队和牌型作为表单默认值；红牌、球队官员和规则要求代理的守门员判罚必须填写另一名代罚球员，无罚时的轻微犯规只换球权。表单只收集结构化判罚内容，停表、罚时、球权和累计判罚仍由计分核心处理。
- 判罚与球队暂停记录的更正、作废统一由 `RecordCorrectionDialog.vue` 处理。更正判罚时可核定球队、犯规主体、牌型、代罚者及剩余罚时；更正暂停时可改记球队。作废和更正都必须填写依据并保留原记录，取消弹窗后仍保持停表。
- 比赛运行数据更正由 `MatchCorrectionDialog.vue` 处理，可分别更正比赛剩余时间、复赛进攻时间、总比分或单条未释放罚时；节间不允许修改比赛钟和进攻钟。每次只提交所选项目及裁判依据，保存和取消后都保持停表。
- 已结束比赛的最终比分更正由 `FinalScoreDialog.vue` 处理，必须填写裁判依据；保存时只调整总比分并保留原进球明细，随后由计分核心重新判断比赛结束、平局加时和金球状态，不自动开始计时。
- `GoldenGoalReviewDialog.vue` 用于“加时金球进球核对”，不是普通进球录入。仅当金球制下因补录、撤销或比分更正造成多条加时进球或比分冲突时，由裁判确认首个有效进球（或全部无效）和最终比分；其他加时进球标记无效但保留原记录。
- 页面顶部、导航、计时台、规则页、记录页及所有业务弹窗现均由 `ScoreboardView.vue` 直接组织，不再使用旧 HTML 外壳或 `Teleport` 挂载点。弹窗名称、当前页面、记录筛选以及各弹窗入口参数由 Vue 单向持有，控制器仅返回动作产生的界面意图，不再保存或反向同步界面状态。弹窗由 Vue 和 `BaseDialog.vue` 管理打开、关闭、焦点恢复及 Esc 键；业务组件通过 `useScoreboardActions` 直接调用统一动作服务，不使用 `data-action` 或根节点点击冒泡解析。各弹窗通过 `useScoreboardForm` 直接提交响应式草稿并自行展示错误，规则字段校验时由表单展开对应分组；控制器只接收表单业务类型、结构化数据和 Vue 明确提供的当前弹窗上下文，不读取 DOM 表单 ID 或 `FormData`。全局动作错误由 Vue 响应式渲染并聚焦，文件下载和全屏调用集中在 `src/utils/browser.js`；控制器只返回结构化下载或全屏意图，不得查询、创建或修改界面 DOM。计分引擎和控制器不得注册或移除组件根节点上的原生 DOM 监听，也不得向组件 DOM 动态插入可用性提示。
- HTTP 请求统一通过 `la-scoreboard-vue/src/api/request.js`，负责 API 根路径、Bearer token、超时、Result 解包以及业务错误；组件不得直接散写 `fetch`。
- 后端接口统一封装在 `la-scoreboard-vue/src/api`，组件和 composable 只调用 API 方法，不自行拼接接口地址。
- 当前开发阶段尚未接入记分员登录码，`dev` 环境通过 `edus.auth.scoreboard-permit-all=true` 仅允许 `/scoreboard/**` 接口免登录，并以系统用户 `0` 完成自动填充；`test` 和 `prod` 必须保持关闭。后续启用记分员授权时关闭该开关，不得删除现有 token 与签名认证能力。
- MySQL 与 Redis 的主机、端口、库名、账号、密码、SSL 和时区等连接参数统一在 `application.yml` 中通过环境变量读取，不得在 `application-dev.yml`、`application-test.yml` 或 `application-prod.yml` 重复定义或写死地址；各 profile 只保存 Hikari、Redis Lettuce 连接池容量以及日志、权限等环境差异。Docker 的 `.env.test` 由 `docker run --env-file` 注入，修改文件后必须重新创建容器；Docker Service 必须在服务配置中单独传入相同环境变量。
- Vue 页面启动时分别读取启用的 `team_age_group` 字典项、规则模板和比赛列表；组别下拉使用字典项，模板再按选中的 `item_value` 筛选。地址未指定 `matchId` 时，列表按后端顺序读取第一条最近比赛并加载详情；列表为空时仍展示数值归零的完整计分台，只开放新建比赛、主题切换、全屏和页面页签。新建服务器比赛必须提交 `ageGroup` 和完整规则快照，后端必须再次校验它是启用的字典值，`groupRuleId` 可为空。自定义规则选择另存模板时同时提交 `saveAsTemplate=true` 与 `templateName`，由后端在建赛事务内新增模板并回填比赛关联。通过 `?matchId={比赛ID}` 加载的比赛，其计时台操作统一串行调用状态保存接口，一次提交最新状态、本次新增的多条事件和现有罚时状态；响应中的事件、罚时数据库 ID 与下一版 `stateVersion` 必须回填前端，避免重复新增和乱序覆盖。未开赛比赛的基础信息与本场规则分别调用信息接口和规则接口，成功后重新读取权威详情。
- Vue 开发服务器固定使用 `http://localhost:5173`，并默认将 `/api` 代理到 `http://localhost:8080`；其他环境使用 `VITE_API_BASE_URL` 覆盖接口地址。
- 可手动上传的部署包位于 `deploy/app-score`：`frond/dist` 保存 `la-scoreboard-vue` 构建产物，Maven 构建产物、`back` 部署目录和镜像内部统一使用 `lacrosse-scoreboard.jar`，不得因环境不同修改 JAR 文件名。`app-score/Dockerfile` 将二者制作成同一个镜像；容器内 Nginx 通过 `80` 端口提供前端并将 `/api` 转发到同容器 Java `8080` 端口，镜像不携带单独的 `nginx.conf` 文件。Dockerfile 默认设置 `JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -Dfile.encoding=UTF-8`，普通容器启动时可由 `.env.test` 覆盖，其他部署方式可通过容器环境变量重新设置 JVM 参数。`.env.test` 保存环境连接参数。`deploy/build-package.sh` 只在安装 Node.js 和 Maven 的开发机执行，必须运行前端测试与构建以及 Java 测试，再更新 `app-score/frond/dist` 和 `app-score/back/lacrosse-scoreboard.jar`，不在开发机制作镜像归档。服务器只上传同一个 `app-score`，可在任意目录通过 `chmod +x deploy.sh` 授予权限后执行 `./deploy.sh`；脚本必须检查 `back` 中的 JAR 和 `frond/dist/index.html`，并要求用户手动输入非空且格式合法的镜像版本号，以 `app-score` 为构建上下文生成 `hub.lo/jx/lascore:{版本号}` 镜像。镜像构建后必须先询问是否通过 `docker service update --image hub.lo/jx/lascore:{版本号} --force msdev_lascore_dev` 更新 Docker Service；选择更新时随后执行 `docker service logs msdev_lascore_dev` 并结束，Service 已有环境变量保持不变。选择不更新 Service 时，再询问是否启动普通 Docker 容器；普通容器默认不启动，选择启动时才读取 `.env.test`、映射一个对外端口并启动 `lacrosse-scoreboard` 容器。普通容器启动后可直接通过 `http://服务器IP:端口` 访问完整项目，也可由服务器已有的外层 Nginx 转发到该单一端口。

## 9. 实施检查清单

- 是否先查看了相关实现、测试、`db/scoreboard.sql` 和本指引？
- 新字段是业务需要，还是仅为了复刻某个页面/Excel？
- 是否正确区分可复用规则模板和本场规则快照？
- 是否正确使用字典 `type_code` 与 `item_value`？
- 是否避免跨平台外键，并保留必要的历史快照？
- 计时操作是否前端即时响应、后端事务落库且可恢复？
- 更正/作废是否保留审计记录和依据？
- 新增代码值是否同步更新 SQL 注释、本文件和测试？
- 是否运行了相关测试和构建？
