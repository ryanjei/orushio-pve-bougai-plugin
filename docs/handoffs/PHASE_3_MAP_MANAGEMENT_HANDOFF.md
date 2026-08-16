# Phase 3 マップ管理・セットアップ handoff

## 最終コミット

`phase/3-map-management` のPush済みHEADを正とする。SHAは完了報告に記載する。

## 実装内容

- Phase番号を、Phase 2の受入実績を反映した1〜7へ整合した。
- Java版ワールドZIPの検証、原本登録、MapProfile永続化を追加した。
- 原本からゲーム・セットアップ用の所有付き一時ワールドを作成し、安全に回収する処理を追加した。
- 登録マップの一覧、有効化、次回1回固定、削除、セットアップを管理API/UIへ追加した。
- Paperメインスレッドでのロード、管理者転送、専用アイテム配布、アンロードを追加した。
- ゲーム内クリックによる地点・Cuboid登録、保存・破棄を追加した。

## 主な変更ファイル

- `src/main/java/com/ryanjei/orushio/pve/map/*`
- `src/main/java/com/ryanjei/orushio/pve/paper/MapSetupListener.java`
- `src/main/java/com/ryanjei/orushio/pve/paper/PaperMapWorldGateway.java`
- `src/main/java/com/ryanjei/orushio/pve/http/AdminHttpServer.java`
- `src/main/resources/web/index.html`
- `src/main/resources/web/app.js`
- `src/test/java/com/ryanjei/orushio/pve/map/*`
- `src/test/java/com/ryanjei/orushio/pve/http/MapAdminHttpServerTest.java`
- `docs/05_DEVELOPMENT_PLAN.md`
- `docs/06_ACCEPTANCE_TESTS.md`

## MapProfile構造

- 識別・管理: `mapId`, `displayName`, `enabled`, `templateDirectory`, `createdAt`
- 地点: `farmSpawn`, `combatEntry`, `normalCoreCandidates`, `finalCore`, `finalEntryTrigger`, `shopPoints`
- 範囲: `farmRegion`, `gateRegions`, `finalRegion`, `enemyZones`, `resourceZones`, `checkpoints`
- 必須判定は製品仕様に従い、通常コア候補3個以上、ショップ1個以上、必須地点・範囲を要求する。
- 不足があるプロファイルの有効化はDomainで拒否する。

## ZIP安全対策

- Zip Slip、`../`、絶対パス、ドライブパスを拒否する。
- ZIP中央ディレクトリのUnix modeを検査し、シンボリックリンクを拒否する。
- ZIPサイズ512 MiB、展開後2 GiB、20,000ファイルを既定上限とする。
- 空、不正、`level.dat`を確認できないZIPを拒否する。
- ワールド本体はZIP直下または1階層下だけを探索する。
- ランダムなステージング領域へ展開し、失敗時に登録先とステージングを回収する。

## 原本保護・所有確認

- 原本は `maps/<mapId>/template` に保存し、Paperへ直接ロードしない。
- マップディレクトリの `.orushio-map-owner` がmapIdと一致する場合だけ削除する。
- 一時ワールドには `.orushio-world-owner` を置き、mapId、用途、UUID所有IDを保存する。
- 削除時はディレクトリ、接頭辞、所有マーカーの全項目を照合する。
- 起動時回収は、接頭辞と有効な所有マーカーを持つ一時ワールドだけを対象とする。

## Paperロード・アンロード方式

- 原本コピーと削除はHTTP/Paperメインスレッド外で行う。
- `GameThreadExecutor`を介してPaperメインスレッド上でワールドをロードする。
- セットアップ管理者を転送し、PDC所有タグ付きの専用アイテムを配布する。
- 終了時は参加プレイヤーをロビーへ戻し、保存せずアンロードしてから所有確認付きで削除する。

## セットアップ操作方式

- 管理画面でマップとオンライン管理者を選び、セットアップを開始する。
- 管理画面で登録フィールドを選択する。
- 専用アイテムの右クリックで地点または範囲2点目、左クリックで範囲1点目を登録する。
- OPかつ `orushio.pve.admin` 権限を持つ、開始時に指定された管理者だけが操作できる。
- 保存時はMapProfileへ反映し、破棄時は既存Profileを変更しない。

## API一覧

- `GET /api/v1/maps`
- `POST /api/v1/maps/import`
- `PUT /api/v1/maps/enabled`
- `PUT /api/v1/maps/next`
- `DELETE /api/v1/maps`
- `GET /api/v1/maps/setup`
- `POST /api/v1/maps/setup/start`
- `PUT /api/v1/maps/setup/field`
- `POST /api/v1/maps/setup/save`
- `POST /api/v1/maps/setup/discard`

全mutationはPhase 1/2の認証、Host、Origin、CSRF、診断モード拒否を共通経路で継承する。

## テスト一覧・結果

- Phase 3関連: ZIP安全性、原本保護、一時コピー、所有削除・異常終了回収、MapProfile、YAML schema、選択、セットアップ、管理API/UIを確認し成功。
- 全体回帰: 93件成功、0件失敗。
- `gradlew build --no-daemon`: 成功。

## 実Paper E2E結果

- Paper 1.21.11 build 132を公式配布元から取得し、SHA-256を照合した。
- 生成したプラグインJARのロード・有効化、管理HTTP bind、Paper Ready、`stop`による正常停止を確認した。
- Minecraftクライアントを利用できない環境のため、オンライン管理者を伴うZIP登録から地点・範囲クリック、保存/破棄までのプレイヤー操作E2Eは未実施。
- ZIP展開、原本不変、一時コピー、所有削除、地点・範囲、保存・破棄は自動テストで確認した。

## 既知の制限

- ZIP上限値は正本を512 MiB / 2 GiB / 20,000ファイルへ更新し、HTTP受信とimporterが同じ定数を参照する。現時点で管理画面からの変更機能は設けていない。
- `templateRevision`、`validationStatus`、`updatedAt`はPhase 6の実マップ更新・再検証で追加する将来フィールドと正本へ明記し、Phase 3 schemaVersion 1には保存しない。

## ユーザー向け手動E2E手順

1. 管理画面へログインする。
2. `level.dat`を含む小規模なJava版テストワールドZIPを登録する。
3. オンラインかつOP・`orushio.pve.admin`権限を持つ管理者を選び、マップセットアップを開始する。
4. 一時ワールドへ転送され、専用のマップ設定ツールを受け取ることを確認する。
5. 管理画面で`farmSpawn`を選び、ツールで単一点を登録する。
6. 管理画面で`farmRegion`を選び、左クリックで1点目、右クリックで2点目を登録する。
7. 管理画面のセットアップ状態・不足項目へ反映されることを確認する。
8. 保存終了する。
9. ロビーワールドへ帰還することを確認する。
10. `orushio_setup_*`一時ワールドがアンロード・削除されたことを確認する。
11. 登録原本のファイル内容・更新時刻が変化していないことを確認する。
12. 同じマップで再度セットアップを開始する。
13. `farmSpawn`または`farmRegion`を別の値へ変更する。
14. 破棄終了する。
15. 再読込み後のMapProfileが手順8で保存した内容から変化していないことを確認する。
- 次回固定状態はプロセス内の1回指定であり、再起動をまたいで永続化しない。
- 実プレイヤーを伴うクリックE2Eは上記環境制約により手動確認が必要。

## 意図的未実装

- PvE、敵、コアHP/攻撃、ゲート開放、ショップ取引、資源生成、妨害、YouTube、OBS、ランチャー。

## ChatGPT重点レビュー箇所

- ZIP中央ディレクトリによるシンボリックリンク拒否と各上限値。
- 所有マーカーを使った登録マップ・一時ワールド削除境界。
- 一時ワールド回収を起動時に非同期実行する責務分離。
- MapProfileの必須判定と`docs/04_DATA_AND_API.md`フィールド名の整合。
- セットアップ開始・保存・破棄時のDomain状態遷移とPaperアンロード順序。

## 初回レビュー集約修正

- 起動時回収開始前に作成ゲートを閉じ、起動前残骸のスナップショット回収完了までsetup/run作成を拒否する。失敗後も作成を拒否し、診断警告とサーバーログへ表示する。
- setup保存・破棄はMAP_SETUP状態とsessionIdを破壊処理前に検証する。保存後のcleanup失敗時は従来Profileへrollbackし、draftとMAP_SETUP状態を維持する。
- 管理者のonline・OP・権限はworldロード前に検証する。ロード後失敗はPaper側と呼出側の双方でアンロードを試行し、アンロード不能時は所有ディレクトリを削除しない。
- `farmSpawn`、`combatEntry`、`finalCore`、`finalEntryTrigger`および`farmRegion`、`finalRegion`は再登録時に置換する。その他の複数地点・範囲は追加する。
