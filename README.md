# orushio-pve-bougai-plugin

Minecraft Java版（Paper）向けの「妨害＋PvE＋視聴者参加型」企画ツール。

現在は実装前の仕様確定段階である。最初の完成版では、YouTube連携を除き、待機・参加・マップ選択・準備・PvE・コア攻略・妨害・死亡復帰・クリア・原状復帰までを一通り遊べる状態にする。

## 重要方針

- 通常運用ではMinecraftコマンド、PowerShell、CLIを使用しない。
- 設定、マップ登録、ゲーム操作は日本語のWeb管理画面から行う。
- マップごとに原本ワールドを保存し、ゲーム開始時に一時ワールドを生成する。
- YouTube連携は後続Phase。妨害機能は先に実装し、管理画面から手動テストできるようにする。
- 実装はCodex、仕様・レビュー・受入判定はChatGPTが担当する。

## Codexが最初に読む順番

1. [AGENTS.md](AGENTS.md)
2. [固定技術・運用判断](docs/00_FIXED_DECISIONS.md)
3. [仕様書索引](docs/README.md)
4. 作業対象Phaseの正式依頼書

## 文書

- `docs/00_FIXED_DECISIONS.md`：Codexが独自選定しない固定値
- `docs/01_PRODUCT_SPEC.md`：ゲーム全体仕様
- `docs/02_ADMIN_UI_SPEC.md`：管理画面とセットアップ操作
- `docs/03_ARCHITECTURE.md`：内部構成と安全設計
- `docs/04_DATA_AND_API.md`：保存データと管理API契約
- `docs/05_DEVELOPMENT_PLAN.md`：Codex使用量を抑える工程
- `docs/06_ACCEPTANCE_TESTS.md`：受入条件・テスト項目
- `docs/requests/PHASE_1_CODEX_REQUEST.md`：最初のCodex正式依頼書
