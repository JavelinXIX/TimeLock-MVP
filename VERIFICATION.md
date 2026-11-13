# ローカル検証結果（2026-11-13 UTC、GitHub登録前）

- Temurin JDK 21 / Gradle 8.13 / AGP 8.13.2 / Android SDK 37.0。
- `testDebugUnitTest lintDebug assembleDebug assembleRelease`: BUILD SUCCESSFUL。
- 単体テスト: 6件成功、失敗0件。日単位の不変上限、翌日反映、入力上限、日付境界、複数Activityの重複排除、消灯後の再開を検証。
- Android Lint: エラー0、警告7（依存更新案内、KTX利用提案、言語リソースに関する警告）。ビルド時にAGPのAPI 37公式検証範囲に関する警告あり。
- デバッグAPK: apksigner verify成功（v2署名）。配布用の固定署名ではありません。
- リリースAPK: 一時的な検証専用鍵で署名し、apksigner verify成功。versionCode 1 → 2で署名証明書が同一であることを確認。検証用鍵とリリースAPKは配布対象から除外・削除。
- APKのcompileSdk/targetSdkは37、minSdkは30とaaptで確認。
- Galaxy Z Fold7 / Android 17の実機・エミュレーター実行は未検証。
- GitHubリポジトリ未指定のため、GitHub Actions上の実行・Artifacts保存は未検証。workflowと署名Secretsの手順を同梱。

詳細な最終ビルドログは `verification/build.log` を参照してください。

## GitHub Actionsでの検証

### Secrets登録後の配布用ビルド

- 実行: https://github.com/JavelinXIX/TimeLock-MVP/actions/runs/43437833741
- 対象コミット: `ce833aab4e22225379fbb5a19072864deb4ca396`
- テスト・Lint・デバッグAPK生成・固定鍵でのリリースAPK署名・Artifacts保存を含め、workflow全体が成功。
- 配布APK: [TimeLock-install-3](https://github.com/JavelinXIX/TimeLock-MVP/actions/runs/43437833741/artifacts/14044370967)。ZIPを展開し、`app-release.apk`をインストールしてください。
- Artifactsから取得したAPKの`apksigner verify --verbose`が成功（v2署名）。versionCode 3、compileSdk/targetSdk 37を確認。
- ZIPのSHA-256: `a1dfae490d0acdea7bd68055ebeb4e948cb97100a6234eeb488435d3839d9f4e`（GitHubのArtifact digestと一致）。
- 今後も同じ署名鍵を維持してください。確認用debug APKから移行するときだけ、署名が異なるためアンインストールが必要です（設定も消えます）。
- 実機の利用時間計測・ブロック動作は未検証です。

### Secrets登録前の初回検証

- リポジトリ: https://github.com/JavelinXIX/TimeLock-MVP
- 実行: https://github.com/JavelinXIX/TimeLock-MVP/actions/runs/43428937777
- 対象コミット: `00f8f53f87957607f5a7ccd6bb065b25f68a09a5`
- `Verify and build preview`（単体テスト・Lint・assembleDebug）: 成功。
- `preview-apk-2`のArtifactsアップロード: 成功。ZIPを展開して`app-debug.apk`をインストールできます。
- `ANDROID_KEYSTORE_BASE64`が未登録（実行時に空）だったため、固定署名鍵の復元工程は失敗し、配布用リリースAPKは未生成です。署名Secretsを確認・登録して再実行してください。workflow全体の表示は失敗になります。
- この接続にはActions Secrets一覧の参照権限がありません。秘密鍵・パスワードはチャットやソースに含めず、リポジトリ所有者が[Actions Secrets設定](https://github.com/JavelinXIX/TimeLock-MVP/settings/secrets/actions)から管理してください。
- 初回実行で判明したSDKセットアップの廃止済み`tools`パッケージ参照は修正済みです。
