# TimeLock MVP

[GitHub Actions](https://github.com/JavelinXIX/TimeLock-MVP/actions/workflows/android.yml) / [GitHub上の検証結果](VERIFICATION.md#github-actionsでの検証)

Kotlin + Jetpack ComposeによるAndroidのアプリ利用時間制限。Galaxy Z Fold7 / Android 17を利用想定とし、minSdk 30 / compileSdk・targetSdk 37でビルドします。Android 17専用APIは使いません。AGP 8.13.2の公式検証上限はAPI 36.1のため、API 37.0でのビルド時に互換性の警告が出ます。**Galaxy Z Fold7 / Android 17実機では未検証**です。

## 仕様

- ランチャーに表示されるアプリごとに1〜1440分の上限を設定します。
- **初回も含め、設定は翌日から有効**です。0分は翌日以降の制限解除です。
- ローカル日付ごとに上限を永続保存し、当日の上限は編集・延長・解除できません。翌日以降の予定だけ変更できます。
- 翌日0時を境に新しい日付の使用量・上限を利用します。端末が休止中なら次の実行時に反映します。正確なアラームは不要です。
- UsageStatsManagerのACTIVITY_RESUMED/PAUSED/STOPPED等から、その日のフォアグラウンド時間を計算します。日付境界は端末のタイムゾーンです。日付をまたぐセッションは前日分のイベントを読み、今日の部分のみ集計します。
- AccessibilityServiceで画面遷移と約1秒間隔の上限チェックを行い、超過アプリからホームへ戻してユーザー補助オーバーレイを表示します。上限に達したアプリはその日の再起動時にも対象です。
- ホーム、設定、本アプリは閉じ込めを避けるため対象一覧から除外します。ネットワーク送信、画面内容取得、入力取得は行いません。

## Androidの権限と限界

これは個人向けの抑止MVPです。通常アプリに他のパッケージを停止・無効化する権限はありません。ユーザー補助によるホーム移動と表示で利用を妨げるもので、OSの起動禁止ではありません。検知・集計には遅延があり、短時間表示や操作できる場合があります。

利用状況アクセスとユーザー補助の両方をユーザーが許可する必要があります。いずれかの解除、強制停止、アンインストール、データ消去、セーフモード、別プロファイル、端末の日時・タイムゾーン変更で回避できます。Samsungの省電力設定・OSによるサービス停止・再起動後のサービス復帰にも影響されます。データ削除・時計改変を含む「絶対に当日は変更できない」保証は通常権限では不可能です。

Foldの分割画面・ポップアップ・PiPでは複数アプリが表示され、イベントの順序やフォーカスによって計測・遮断が不完全になることがあります。MVPは通常の単一アプリ利用を中心とします。OSの利用時間集計と一致する保証はありません。前日より前から継続したセッションや失われたイベントも正確には復元できません。

より強い管理には、初期設定時にDevice Ownerとして端末を管理する別構成とDevicePolicyManager等が必要です。一般のインストールだけではDevice Ownerにはなれません。Google Play公開時はユーザー補助APIの申告・説明・同意など、最新のポリシー確認が必要です。

## ビルド

JDK 21、Android SDK platform 37.0とbuild-tools 36.0.0を用意します。

```sh
printf 'sdk.dir=/path/to/android-sdk\n' > local.properties
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`。これは開発用です。CIごとに生成されるdebug署名は継続配布に使わないでください。

## GitHub Actionsと署名鍵

`.github/workflows/android.yml`はpush/PR/手動実行で検証してpreview APKをArtifactsに保存します。push/手動では固定鍵で署名した配布APKも生成します。**Secrets未設定なら配布工程は明示的に失敗**します。無署名APKやランダム鍵へのフォールバックはしません。PRには配布鍵を渡しません。

最初に一度だけ信頼できる端末または管理環境で鍵を生成してください。鍵はリポジトリやチャットに貼り付けず、暗号化してパスワードと別にバックアップします。

```sh
keytool -genkeypair -keystore timelock-release.jks -alias timelock -keyalg RSA -keysize 3072 -validity 10000
base64 -w 0 timelock-release.jks > timelock-release.base64
```

GitHubリポジトリのSettings → Secrets and variables → Actionsに次を登録します（スマートフォンのブラウザからも設定できます）。鍵生成自体にはkeytoolを使える信頼済み環境が必要です。スマートフォンだけでのダウンロード・インストールに鍵生成ツールは不要です。

|Secret|内容|
|---|---|
|ANDROID_KEYSTORE_BASE64|鍵ファイルのBase64（改行なし）|
|ANDROID_STORE_PASSWORD|キーストアのパスワード|
|ANDROID_KEY_ALIAS|timelock|
|ANDROID_KEY_PASSWORD|鍵のパスワード|

毎回同じapplicationId、鍵、aliasを使います。配布のversionCodeはgithub.run_numberです。同じworkflowを継続使用し、別workflowへの移行時は既存の最大versionCodeより大きい番号にしてください。再実行は同じ番号になります。鍵を失うと既存アプリを更新できず、アンインストールすると制限設定も消えます。Secretsを読めるworkflowを変更できる権限を絞り、信頼できない変更をマージしないでください。鍵・パスワードのログ出力は禁止です。

## スマートフォンだけでインストール

1. このプロジェクトをGitHubリポジトリに置き、上記Secretsを設定します。
2. GitHubにログインしたスマートフォンのブラウザでActions → Android APK → Run workflowを実行します。
3. 成功した実行のArtifactsから **TimeLock-install-番号** をダウンロードしてZIPを展開します。Artifactsは保持期間内に取得してください（GitHub標準は通常90日）。
4. 中の`app-release.apk`を開き、ブラウザ/ファイル管理アプリに「不明なアプリのインストール」を許可します。Samsungの自動ブロッカーが妨げる場合は端末の案内に従います。
5. TimeLockを開き、利用状況アクセスとユーザー補助のTimeLockを有効化します。サイドロードの制限付き設定が表示された場合はアプリ情報メニューから制限付き設定を許可します。
6. アプリの翌日以降の上限を設定します。今日の上限は変更されません。
7. 更新は新しいTimeLock-installのAPKを既存アプリに上書きインストールします。preview APKと配布APKは署名が異なるため混用しません。

## 実機受け入れチェック

翌日用に1分を設定し、翌日使用して超過・再起動時の遮断を確認。当日の編集で上限が変わらないこと、0時後の解除、日付をまたぐ利用、端末再起動、権限解除時の挙動を確認してください。Foldの折りたたみ/展開、分割画面、PiP、省電力時は別途確認が必要です。
