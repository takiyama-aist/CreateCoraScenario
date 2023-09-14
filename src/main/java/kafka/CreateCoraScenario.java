package kafka;

import picocli.CommandLine;
import utils.Configuration;
import utils.Tuple2;

import java.io.*;
import java.util.*;

public class CreateCoraScenario {

    @CommandLine.Option(names = "-D")
    void setProperty(Map<String, String> props) {
        props.forEach((k, v) -> System.setProperty(k, v == null ? "" : v));
    }

    public static void main(final String[] args) throws IOException {
        Configuration params = new Configuration(args);
        System.out.println(params);
        String filePathCites = params.filePathCites;
        String filePathContent = params.filePathContent;
        String outScenarioPath = params.outScenarioPath;

        Map<Integer, List<Integer>> mapEdge = getNodeMapFromCora(filePathCites);// 本来のエッジ情報
        Map<Integer, List<Integer>> mapAddedEdge = new HashMap<>();             // 作成済みのエッジ情報
        List<Integer> listAddedNode = new ArrayList<>();                        // 検出済みノードリスト

        try (FileWriter fw = new FileWriter(outScenarioPath, false)) {
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(fw))) {
                try (FileReader fr = new FileReader(filePathContent)) {
                    try (BufferedReader br = new BufferedReader(fr)) {
                        String line;
                        Integer nodeId;
                        // 1行抽出
                        while ((line = br.readLine()) != null) {
                            // ノード増イベント
                            nodeId = outNodeIncEvent(line, pw);
                            // エッジ増イベント
                            outEdgeIncEvent(pw, nodeId, mapEdge, mapAddedEdge, listAddedNode);
                            listAddedNode.add(nodeId);
                        }
                    }
                }
            }
        }
    }

    /**
     * ノード増イベント出力処理。<br>
     * ノード増イベントをファイルに出力する。
     *
     * @param line 読み込んだ1行
     * @param pw PrintWriter
     * @return ノードID
     * @throws IOException ファイルの読み書きに失敗した場合
     */
    private static Integer outNodeIncEvent(String line, PrintWriter pw) throws IOException {
        // タブまたはスペース区切り
        String[] texts = line.split("[\\x20\\t]+");
        String nodeId = texts[0].trim();
        Tuple2<List<String>, List<String>> tuple2 = getNumFeatureListTextList(texts);
        // ノード増イベント
        pw.print("0");
        pw.print(",");
        pw.print(nodeId);
        pw.print(",");
        for (String feature : tuple2.f0) {
            pw.print(feature);
            pw.print(" ");
        }
        pw.print(",");
        pw.println(tuple2.f1.get(0));
        return Integer.parseInt(nodeId);
    }

    /**
     * エッジ増イベント出力処理。<br>
     * 新規に検出されたノードであれば、エッジがあれば出力する。
     *
     * @param pw PrintWriter
     * @param nodeId 追加されたノードID
     * @param mapEdge 本来のエッジ情報（ノードIDと参照しているノードIDリストのMap）
     * @param mapAddedEdge 既に作成済みのエッジ情報（ノードIDと参照しているノードIDリストのMap）
     * @param listAddedNode 検出済みのノードリスト
     */
    private static void outEdgeIncEvent(PrintWriter pw, Integer nodeId, Map<Integer, List<Integer>> mapEdge,
                                        Map<Integer, List<Integer>> mapAddedEdge, List<Integer> listAddedNode) {
        // (A) 本来のエッジ情報（cora.citesから取得）のMap (mapEdge)
        // (B) 作成済みエッジ情報Map (mapAddedEdge)

        // ノードが(A)参照元に含まれる
        if (mapEdge.containsKey(nodeId)) {
            // (B)に含まれない場合は追加する
            if (!mapAddedEdge.containsKey(nodeId)) {
                List<Integer> nodeRefList = new ArrayList<>();
                List<Integer> listOrgNode = mapEdge.get(nodeId);
                for (Integer node : listOrgNode) {
                    if (listAddedNode.contains(node)) {
                        nodeRefList.add(node);
                    }
                }
                // 参照先ノードがあればエッジ増イベント書き込み
                if (nodeRefList.size() > 0) {
                    writeEdgeIncEvent(pw, nodeId, nodeRefList);
                    mapAddedEdge.put(nodeId, nodeRefList);
                }
            }
        }

        // （A）から取得した全Value（本来の参照先）に検出したノードが含まれるか判定
        Set<Integer> key = mapEdge.keySet();
        for (Integer keyNode : key) {
            List<Integer> nodeRefList = new ArrayList<>();
            // 検出したノードが含まれる
            if (mapEdge.get(keyNode).contains(nodeId)) {
                // 参照元ノードが未検出であれば対象外
                if (!listAddedNode.contains(keyNode)) {
                    continue;
                }

                // そもそも参照元ノードが（B）に無い
                if (!mapAddedEdge.containsKey(keyNode)) {
                    nodeRefList.add(nodeId);
                }
                // 参照元ノードが（B）にある
                else {
                    // 過去のnodeRefListを取得
                    nodeRefList = mapAddedEdge.get(keyNode);
                    // 重複しないように（この判定は意味がない。新たに検出したものなので、過去のnodeRefListには含まれていないはず。）
                    if (!nodeRefList.contains(nodeId)) {
                        nodeRefList.add(nodeId);
                    }
                }
            }
            // 参照先ノードがあればエッジ増イベント書き込み
            if (nodeRefList.size() > 0) {
                writeEdgeIncEvent(pw, keyNode, nodeRefList);
                mapAddedEdge.put(keyNode, nodeRefList);
            }
        }
    }

    /**
     * エッジ増書き出し処理
     *
     * @param pw PrintWriter
     * @param nodeId ノードID
     * @param nodeRefList nodeRefList
     */
    private static void writeEdgeIncEvent(PrintWriter pw, Integer nodeId, List<Integer> nodeRefList) {
        // エッジ増イベント
        pw.print("3");
        pw.print(",");
        pw.print(nodeId);
        pw.print(",");
        for (Integer node : nodeRefList) {
            pw.print(node);
            pw.print(" ");
        }
        pw.println();
    }

    /**
     * cora.citesからノードを取り出しリストに追加する。
     *
     * @param path　cora.citesのパス
     * @return ノードIDと参照しているノードIDのリストのマップ
     * @throws IOException
     */
    private static Map<Integer, List<Integer>> getNodeMapFromCora(String path) throws IOException {
        Map<Integer, List<Integer>> map = new HashMap<>();
        File file = new File(path);
        try (FileReader fr = new FileReader(file)) {
            try (BufferedReader br = new BufferedReader(fr)) {
                String text;
                // 1行抽出
                while ((text = br.readLine()) != null) {
                    try {
                        // タブまたはスペース区切り
                        String[] texts = text.split("[\\x20\\t]+");
                        if (texts.length == 2) {
                            int node = Integer.parseInt(texts[0].trim());
                            int targetNode = Integer.parseInt(texts[1].trim());
                            // 未登録
                            if (!map.containsKey(node)) {
                                List<Integer> list = new ArrayList<>();
                                list.add(targetNode);
                                map.put(node, list);
                            }
                            // 既に登録されている
                            else {
                                List<Integer> list = map.get(node);
                                list.add(targetNode);
                            }
                        }
                    }
                    catch(NumberFormatException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        return map;
    }

    /**
     * numFeatureList、textList取得処理
     *
     * @param texts cora.content1行分の文字列
     * @return numFeatureListとtextListのTuple
     */
    private static Tuple2<List<String>, List<String>> getNumFeatureListTextList(String[] texts) {
        List<String> numFeatureList = new ArrayList<>();
        for (int i = 1; i < texts.length -1; i++) {
            try {
                String s = texts[i].trim();
                int num = Integer.parseInt(s);
                if (s.length() == 0) {
                    continue;
                }
                if (num != 0) {
                    numFeatureList.add((i-1) + ".0");
                }
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        List<String> textList = new ArrayList<>();
        textList.add(texts[texts.length-1].trim());
        return new Tuple2<>(numFeatureList, textList);
    }
}