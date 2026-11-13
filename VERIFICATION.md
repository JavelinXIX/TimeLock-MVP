# 検証結果（2026-11-13 UTC）

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
