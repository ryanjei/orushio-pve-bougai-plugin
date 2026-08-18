# Phase 4.2 マップ実行基盤 handoff

## Runtime Worldとownership

- `GameRuntimeLifecycleStep`が選択済み`MapProfile`を`RuntimeMap`へ解決し、Phase 3の`TemporaryWorldManager`でpurpose=`run`の一時worldを作る。
- ownership markerはmapId、purpose、乱数ownershipId、sessionIdを保持する。削除時はパスと全ownership値が一致したworldだけを対象にする。元テンプレートはコピー元としてのみ読み、ゲーム中の変更はRuntime Worldだけへ入る。
- 起動前から残るsetup worldと無関係なrun worldはPhase 3のsnapshot recoveryで回収する。正常schemaの未完了active-sessionはHard Diagnosticと分離したRecoverable状態としてRECOVERINGへ保存し、そのsessionIdのrun worldだけをsnapshot回収から保護する。破損／未知schema／所有証明不能はHard Diagnosticとして全mutationを拒否する。

## lifecycle、参加者、ロビー

- ACTIVE移行処理のprepareで、Runtime Map解決、worldコピー、Paper world load、自然Mobスポーン無効化、地点検証、オンライン参加者転送の順に実行する。全step成功後だけACTIVEになる。
- world load、gamerule、world border／高さ検証、teleport、unloadは`GameThreadExecutor`経由でPaper main thread上に限定する。worldコピー・削除はPaper main threadで行わない。
- オフライン参加者は開始を妨げずGameSessionへ残る。ACTIVE中のParticipant再接続だけをゲーム開始地点（Phase 3の`combatEntry`）へ戻す。非Participantは転送しない。
- Phase 4.2のロビー実装はRuntime World以外で最初に利用可能な通常worldのspawnを使い、`GameRuntimeGateway`境界の内側へ隔離した。将来の明示Lobby SpawnはこのGateway実装を差し替える。
- 再起動後のcleanupはsessionId、mapId、purpose=`run`が一致するOwnedWorldをmarkerから再発見する。Playerをロビーへ戻し、worldにPlayerがいないことを確認し、unloadしてからownership検証付き削除を行う。全処理成功後だけIDLEへ進む。失敗はRECOVERINGに残り「復旧清掃を再試行」で同じ処理を再試行する。終了時オフラインだった参加者はPhase 4.1のpending cleanupで次回接続時にロビー復帰する。

## Area、Spawn Marker、schema

- `MapProfile.logicalAreas()`は保存済みCuboidを`fieldKey:index`のareaIdへ展開する。Phase 4.2ではAreaの削除によるindex変動を避けるため、その種類にMarkerが残る間はArea削除を拒否する。
- `SpawnMarker`は安定UUID、areaId、BlockPoint、enabledを持つ。座標変更は同じUUIDを維持する。`RuntimeMarkerResolver`は現在RuntimeのArea別enabled Markerだけを返す。
- Markerは所属Area内であることをDraft登録時とRuntime解決時に検証する。Paper準備時にはRuntime Worldの高さとworld borderも検証する。足場・窒息・液体判定はEnemy spawnを実装するPhase 4.6へ残す。
- MapProfile schemaVersionは1のまま、後方互換な任意`spawnMarkers`キーを追加した。Phase 3のschemaVersion 1でキーがなければ空一覧として読む。未知schemaVersion、壊れたMarker、重複ID、存在しないAreaは拒否し、自動上書きしない。

## Draft、API、UI

- Marker追加・座標変更・有効切替・削除は編集中Draftだけを変更する。保存終了で正本へ反映し、破棄終了では保存済みMarkerへ戻る。
- APIは`PUT /maps/setup/marker/select`、`PUT|DELETE /maps/setup/marker`、`POST /maps/setup/marker/teleport`を追加した。既存のlocalhost、認証、Host、Origin、CSRF、diagnostic mode拒否、audit、traceIdを通る。
- Recoverable状態では通常mutationと新規Map Setupを拒否し、`POST /game/lifecycle/abort`の復旧清掃だけを許可する。Hard Diagnosticでは復旧清掃も拒否する。
- Runtime状態JSONの正本keyは`worldState`、`mapState`、`transferState`、`error`で、管理画面も同じkeyを参照する。
- 管理画面はMarkerをArea別`details`へ折りたたみ、件数、短縮ID、座標、有効状態、変更、テレポート、削除を表示する。削除は確認必須。Yaw/Pitchは「向き: 東」「視線: 水平」を主表示し数値を詳細として併記する。
- SetupViewのrevisionを1.5秒ごとに静かに確認し、変更時だけsetupコンポーネントを再描画する。global loading、画面暗転、操作通知の上書きは行わない。

## auditと拡張点

- Runtime作成／失敗／cleanup／失敗、Marker追加／削除／有効切替／テレポート、Participant再接続を監査する。token、secret、座標一覧等の秘密・過剰情報は記録しない。
- Phase 4.3以降は`GameLifecycleStep`、`RuntimeMap`、`RuntimeMarkerResolver`、`GameRuntimeGateway`からRuntime World、Area、Marker、Participant転送、cleanupへ局所的に接続する。

## テストとPreflight

- Domain／Repository：Marker ID、Area所属、enabled resolver、Draft save/discard、schemaVersion 1再読込。
- lifecycle／ownership：テンプレート不変、session ownership、prepare rollback、cleanup失敗保持と再試行、オフライン参加者、再接続、pending cleanup、未知ownership拒否。
- HTTP／UI：共通mutation保護、Marker audit、Area折りたたみ、部分更新、方向表示、Runtime状態表示。
- 最終テスト件数、build、JavaScript構文検査、git diff、Windows/Paper Preflight結果はReview commitの完了報告を正本とする。

## 未確認事項・既知の制限

- Minecraftクライアントを必要とするsetup toolクリック、実Player teleport、Marker teleport UIはCodex環境では未確認とする。
- Phase 4.2はEnemy、コア、ファーム資源、ショップ、妨害を生成しない。
- 明示的なLobby Spawnとチェックポイント復帰は未実装。通常world spawnとcombatEntryをそれぞれ暫定契約として使用する。
