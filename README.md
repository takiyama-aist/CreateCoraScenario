# CreateScenario
## 概要
指定したcoraデータセットファイルからシナリオファイルを作成します。<br>
作成したシナリオはKafkaDataSendプログラムで送信できます。

<hr>

## シナリオファイル記載方法
### 共通
CSV形式で記載する。1行に1イベント記載。  
記載する1行は送信イベントメッセージに対応。
- フォーマット
```
<イベント番号>, <nodeId>(, <パラメータ0>, <パラメータ1>) 
```
    
### 1. ノード増イベント
- フォーマット
```
0, <nodeId>, <numFeatureList>, <textList>
```
- パラメータ
    - nodeId : int型で記述。パラメータrandomNodeIdで指定した値を記述した場合はrandom。
    - numFeatureList : double型数値を空白区切りで記述。パラメータrandomNumFeatureListで指定した値を記述した場合はrandomな数値。randomNumFeatureListと他の数値との混在、randomNumFeatureListの複数記載も可。
    - textList : String型で記述。1文字列のみ。

### 2. ノード減イベント（未使用）
- フォーマット
```
1, <nodeId>
```

### 3. ノード変化イベント（未使用）
- フォーマット
```
2, <nodeId>, <numFeatureList>, <textList>
```
- パラメータ
    - numFeatureList、textListはノード増イベントと同様。どちらか省略可能だが、カンマは省略しない。変化がある方を記述。 

### 4. エッジ増イベント
- フォーマット
```
3, <nodeId>, <nodeRefList>
```
- パラメータ
  - nodeRefList : int型数値を空白区切りで記述。パラメータrandomNodeIdで指定した値を記述した場合は数値はrandom。randomNodeIdとrandomNodeId以外の数値の混在も可能。

### 5. エッジ減イベント（未使用）
- フォーマット
```
4, <nodeId>, <nodeRefList>
```

<hr>

## 起動パラメータ
kafka.confに記載。-Dで指定も可能。  
以下パラメータ。

### 1. kafka.filePathCites
cora.citesのフルパス。
### 2. kafka.filePathContent
cora.contentのフルパス。
### 3. kafka.outScenarioPath
シナリオファイルを生成するパス。


