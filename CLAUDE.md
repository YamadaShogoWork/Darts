# ダーツ練習記録アプリ(Android)

個人用のダーツ練習記録アプリ。クリケット10マーク練習(20・19・18・17・16・15・ブルを各10マーク埋める)の本数を記録し、グラフで上達を見る。

## 最初にやること

- 仕様は `docs/spec/SPEC.md` にある。まず全体を読むこと
- 画面の見た目は、モックアップ https://claude.ai/artifact/9woG88DWPmruoHZjwhcB11 を基準にする(Pixel 8)。コピーは `docs/mockup/` にある
  - ホームは「案F(最初の案×赤黒)」(`docs/mockup/HomeF.dc.html`)を採用。全画面の配色をF案の赤黒に揃える(モックアップのホーム以外の画面は緑だが、色だけ置き換える)
- 開発は `SPEC.md` の「開発の順番」の1から順に進める。各ステップが終わったら動作を確かめ、次に進む前に報告する

## 仕様書からの追加・変更(SPEC.mdより優先)

- **横画面**: 記録入力画面だけ横画面に対応する。スマホをスタンドに立てて、投げながら入力する使い方を想定。左に現在のナンバー・累計、右に大きな入力ボタン。ほかの画面は縦画面のみ
- **集計の日付**: 日ごとの集計は練習を「終了した日時」の日付で行う(SPEC.mdの「startedAt の日付」から変更)。そのため Session に `finishedAt`(ブル完了日時)を追加する。未完了の練習は集計に含めない

## フォルダ構成

- `android/` … Androidアプリ本体(Gradleプロジェクト)
- `docs/spec/` … 仕様書(md / PDF)
- `docs/mockup/` … モックアップのHTMLのコピー
- `docs/tasks.md` … タスク表
- `docs/work_logs/` … 作業ログ(`YYYY-MM-DD.md`)

## 技術

- Kotlin + Jetpack Compose + Room + ViewModel
- グラフ: Vico
- バックアップ: Storage Access Framework + kotlinx.serialization(JSON)
- 対象は Android のみ。Web版・iPhone版は作らない

## 守ること

- ルール判定(10マークの完了、超過の切り捨て、ターンの扱い、取り消し)は画面から分けて、単体テストを書く
- ルール判定は `PracticeRule` のインターフェースにして、将来ほかの練習メニューを足せるようにする
- 普段のデータはRoomに保存する。JSONはバックアップ用で、設定画面のボタンで書き出し・読み込みする
- 音声入力、DARTSLIVE Homeとの連携、クラウド同期、ログインは作らない
- 仕様にないことを足す前に、確認する
