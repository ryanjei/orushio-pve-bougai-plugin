# 設計資料索引

ステータス：**実装開始可能（YouTube連携を除くv1仕様）**

## 確定したスコープ

| 項目 | v1 | 後続 |
|---|---:|---:|
| Paperプラグイン | ○ | |
| 日本語Web管理画面 | ○ | |
| GUIによる設定・ゲーム操作 | ○ | |
| ワールドZIP登録・管理画面選択・一時複製 | ○ | 自動選択は候補 |
| 最大4人参加型PvE | ○ | |
| ファーム・ショップ・装備 | ○ | |
| 設定された必要通常Core数（既定2）＋最終Core | ○ | |
| 敵スポーン・人数補正 | ○ | |
| Inventory維持での死亡復帰 | ○ | Checkpointは候補 |
| 暗闇・浮遊・ホットバー変更・追加ウェーブ | ○（管理画面手動） | |
| YouTubeコメント取得・OAuth | | ○ |
| コメントと妨害のルール編集 | | ○ |
| 外部公開・スマホ遠隔操作 | | ○ |

## 文書間の役割

- [00_FIXED_DECISIONS.md](00_FIXED_DECISIONS.md)：技術・命名・ポート・保存形式の固定値。
- [01_PRODUCT_SPEC.md](01_PRODUCT_SPEC.md)：何を作るか。ゲーム挙動の正本。
- [02_ADMIN_UI_SPEC.md](02_ADMIN_UI_SPEC.md)：ユーザーがどう操作するか。
- [03_ARCHITECTURE.md](03_ARCHITECTURE.md)：どう分割して安全に実装するか。
- [04_DATA_AND_API.md](04_DATA_AND_API.md)：保存形式と内部境界。
- [05_DEVELOPMENT_PLAN.md](05_DEVELOPMENT_PLAN.md)：どの順序でCodexへ依頼するか。
- [06_ACCEPTANCE_TESTS.md](06_ACCEPTANCE_TESTS.md)：完成と判定する条件。

## 未確定扱いにしないための標準値

バランス値は初期標準値を本書群で定義し、管理画面から変更可能にする。数値調整だけを理由にコード修正を依頼しない。

Minecraft/Paperは **Paper 1.21.11・Java 21** に固定する。既存の1.21系マップを前進読込みする想定とし、バージョン更新は機能実装と同じPRに混ぜない。
