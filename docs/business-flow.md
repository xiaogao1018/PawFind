# PawFind 核心业务流程

> 依据：`docs/requirements.md` v1.0  
> 阶段：W1D1 需求分析  
> 范围：宠物走失与寻回、社区帖子发布与管理员审核、AI 寻宠对话

三条流程只描述已确定的业务规则。发现记录与走失记录相互独立。社区帖子先审后发。AI 生成的启事由用户确认后手动发布。

## 1. 宠物走失与寻回

主人发布走失信息后，其他居民可以浏览，并提交目击线索或另行发布发现记录。主人核实线索后，线下自行确认，再把走失状态标记为已寻回。系统负责交换线索，不保证寻回成功。

```mermaid
flowchart TD
  startNode["宠物主人准备发布走失信息"] --> loginCheck{"是否已登录"}
  loginCheck -->|否| loginStop["拒绝发布，需先登录"]
  loginCheck -->|是| fillLost["填写种类、特征、走失时间、区域或大致地点和状态；名称可未知；上传至少一张照片"]
  fillLost --> fieldCheck{"必要字段和图片是否通过校验"}
  fieldCheck -->|否| fieldStop["提示缺少字段、图片格式不合法或文件超限"]
  fieldStop --> fillLost
  fieldCheck -->|是| published["发布成功，状态为寻找中 LOOKING"]
  published --> publicLost["进入公开列表和详情；电话和精确位置默认不公开，只展示模糊区域"]

  publicLost --> reader{"访问者身份"}
  reader -->|游客| guestBrowse["可浏览公开走失和发现信息，不能提交线索或发布记录"]
  reader -->|登录用户| memberChoice{"选择的行动"}

  memberChoice -->|发布发现记录| foundForm["填写发现信息；种类和名称可未知"]
  foundForm --> foundCheck{"发现信息是否通过校验"}
  foundCheck -->|否| foundStop["提示错误，保留在表单"]
  foundCheck -->|是| foundSaved["保存为独立发现记录，不自动关联走失记录"]
  foundSaved --> publicFound["发现记录可被检索和查看；精确联系方式和位置默认不公开"]

  memberChoice -->|提交目击线索| clueTarget{"是否关联一条寻找中的有效走失记录，且必填完整"}
  clueTarget -->|否| clueStop["提交失败并提示"]
  clueTarget -->|是| clueSaved["保存线索：目击时间、地点描述、文字；照片可选。状态为待核实 PENDING"]

  clueSaved --> clueReader{"线索查看者"}
  clueReader -->|走失记录发布者| ownerRead["查看完整线索"]
  clueReader -->|该线索的提交者| selfRead["查看自己提交的内容"]
  clueReader -->|管理员| adminRead["查看完整线索"]
  clueReader -->|无关用户| clueDenied["不能查看完整线索详情"]

  ownerRead --> verify["发布者将线索标为待核实、已核实或无效，并可填写备注"]
  verify --> offline["发布者线下自行确认；接触时注意安全"]
  offline --> foundDecision{"发布者是否确认宠物已寻回"}
  foundDecision -->|是| markFound["发布者把走失状态标记为已寻回 FOUND"]
  foundDecision -->|否| keepLooking["保持寻找中，继续接收新线索"]

  publicLost --> violation{"管理员是否认定走失或发现记录违规"}
  publicFound --> violation
  violation -->|隐藏| hiddenRecord["记录离开公开列表和详情入口，并写入操作日志"]
  hiddenRecord --> restoreRecord{"管理员是否恢复该记录"}
  restoreRecord -->|是| restored["恢复公开并记录日志"]
  violation -->|否| keepPublic["记录保持公开"]
```

补充规则：

- 走失记录和发现记录都只能由发布者编辑。
- 线索必须属于一条有效且处于寻找中的走失记录。
- 联系信息需经本人授权后可见，或通过站内线索转达。首版不新增独立的联系人交换功能。

## 2. 社区帖子发布与管理员审核

新帖初始为 `PENDING`，审核通过前不公开。审核状态与展示状态分开判断。普通成员只能看到 `APPROVED + VISIBLE` 的帖子。

```mermaid
flowchart TD
  enter{"用户是否已登录"} -->|否| guestDenied["游客不能浏览、发布或评论社区帖子"]
  enter -->|是| createPost["填写标题和正文，选择经验分享或求助，图片可选"]
  createPost --> pending["提交成功：审核状态 PENDING，公开入口不可见"]
  pending --> myPosts["作者在我的帖子查看待审核记录"]
  pending --> queue["帖子进入管理员审核队列"]

  queue --> audit{"管理员的审核决定"}
  audit -->|驳回，并填写原因| rejected["审核状态改为 REJECTED，帖子不公开"]
  rejected --> rejectVisible["作者在我的帖子看到驳回原因"]
  rejectVisible --> resubmit["作者修改后重新提交"]
  resubmit --> pending

  audit -->|通过| approved["审核状态改为 APPROVED，展示状态为 VISIBLE"]
  approved --> publicCheck{"访问公开内容的人是否已登录，且帖子为 APPROVED + VISIBLE"}
  publicCheck -->|否| publicDenied["拒绝通过列表、搜索或详情入口查看"]
  publicCheck -->|是| publicPost["登录成员可浏览该帖"]

  publicPost --> commentChoice{"是否发表评论"}
  commentChoice -->|是| commentOk["评论发布后可见；第一版没有楼中楼"]
  commentOk --> commentOwner{"后续处理"}
  commentOwner -->|评论作者删除自己的评论| commentDeleted["该评论被作者删除"]
  commentOwner -->|管理员隐藏违规评论| commentHidden["违规评论被隐藏并记录操作"]
  commentChoice -->|否| noComment["帖子保持可浏览"]

  publicPost --> hideChoice{"管理员是否隐藏这篇已通过帖子"}
  hideChoice -->|是，并填写原因| hiddenPost["状态变为 APPROVED + HIDDEN；公开访问立即失效"]
  hiddenPost --> authorHidden["作者可在我的帖子看到隐藏记录和原因"]
  hiddenPost --> restoreChoice{"管理员是否恢复展示"}
  restoreChoice --> approvedGate{"审核状态是否仍为 APPROVED"}
  approvedGate -->|是| approved
  approvedGate -->|否| restoreDenied["拒绝恢复公开"]

  publicPost --> editChoice{"作者是否修改正文或图片"}
  editChoice -->|是| reaudit["整条帖子暂时下架，审核状态回到 PENDING；修改内容不直接公开"]
  reaudit --> queue
  editChoice -->|否| stayPublic["帖子保持 APPROVED + VISIBLE"]
```

状态结果：

| 审核状态 | 展示状态 | 对普通登录用户 | 对作者 |
| --- | --- | --- | --- |
| `PENDING` | — | 不可见，也不能评论 | 可在我的帖子查看 |
| `REJECTED` | — | 不可见，也不能评论 | 可查看驳回原因，修改后重新提交 |
| `APPROVED` | `VISIBLE` | 可浏览、搜索、查看详情和评论 | 可查看；修改正文或图片后重新审核 |
| `APPROVED` | `HIDDEN` | 不可见，也不能评论 | 可查看隐藏原因 |

隐藏不能代替审核。只有已经通过审核的帖子可以由 `HIDDEN` 恢复为 `VISIBLE`。

## 3. AI 寻宠对话

登录成员通过多轮对话获得寻宠建议、信息整理、启事草稿或一般养宠回答。模型不可用时，只影响 AI 对话，不影响走失发布和社区浏览。

```mermaid
flowchart TD
  aiEnter{"成员是否已登录"} -->|否| aiDenied["拒绝使用 AI 对话"]
  aiEnter -->|是| chat["进入聊天页并发送消息；保留多轮上下文"]
  chat --> modelCall{"大模型调用结果"}
  modelCall -->|超时、额度不足或服务异常| aiError["显示明确错误，允许重试；页面不卡死，也不暴露密钥"]
  aiError --> coreStill["走失信息发布、线索提交和社区浏览仍可使用"]
  modelCall -->|成功返回| intent{"本轮用户意图"}

  intent -->|寻找建议| advice["根据用户已提供的种类、走失时间地点和习性，生成分步骤建议"]
  advice --> limitA["建议不承诺寻回结果，不把未经核实的线索说成事实"]
  limitA --> continueChat["用户可继续追问"]
  continueChat --> chat

  intent -->|整理走失信息| missing{"种类、特征、时间或区域等关键信息是否齐全"}
  missing -->|否| askMore["追问缺失项，或把该项标为未知"]
  askMore --> chat
  missing -->|是| organized["整理用户已经提供的特征和走失时间地点"]
  organized --> chat

  intent -->|生成寻宠启事| draft["生成结构化、可复制的启事草稿"]
  draft --> userConfirm{"用户是否确认草稿内容"}
  userConfirm -->|否| chat
  userConfirm -->|是| manualPublish["用户自行进入走失发布流程，人工确认后手动发布"]

  intent -->|一般养宠问题| healthRisk{"是否涉及高风险健康问题"}
  healthRisk -->|是| vetTip["提供一般性信息，并提示咨询兽医"]
  healthRisk -->|否| generalAnswer["回答一般性养宠问题，并说明回复不能替代专业意见"]
  vetTip --> chat
  generalAnswer --> chat
```

AI 约束：

- 只依据用户已经提供的信息生成建议和启事。
- 缺失信息继续询问或标为未知。
- 启事草稿停止在可复制文本，发布动作由用户完成。
- 首版 AI 不查询业务数据、不执行审核，也不删除或隐藏内容。
- 大模型 API Key 只放在服务端配置中。
