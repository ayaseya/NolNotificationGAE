package com.ayaseya.nolnotificationgae;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;

@SuppressWarnings("serial")
public class ScheduleTask extends HttpServlet {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private Document document;
	private static final String ENTITY_KIND = "Jsoup";
	private static final String ENTITY_KEY = "Document";
	private static final String ACCESS_KEY_FIELD = "Pre_HTML_Data";
	// スクレイピングするページのURLを指定します。
	private static final String URL = "https://www.gamecity.ne.jp/nol/news/index.htm";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		logger.info("ScheduleTaskが呼び出されました");

		resp.setContentType("text/plain;charset=UTF-8");

		// 指定したページをJsoupでスクレイピングする
		// http://ja.wikipedia.org/wiki/%E3%82%A6%E3%82%A7%E3%83%96%E3%82%B9%E3%82%AF%E3%83%AC%E3%82%A4%E3%83%94%E3%83%B3%E3%82%B0
		try {
			document = Jsoup.connect(URL).get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// spanタグ内のaタグ要素を取得します。
		// <span><a>hoge</a><span>
		// 実行結果→<a>hoge</a>
		Elements titles = document.select("span a");

		// aタグ内の文字列を取得します。
		// <a>hoge</a>
		// 実行結果→hoge
		String currentHTML = titles.text();

		/**
		 * 
		 * 変更ありのセクションに移動予定です。
		 * ============ここから============
		 * 
		 **/
		Elements dates = document.select("tr td font strong");

		ArrayList<String> HTML = new ArrayList<String>();

		// 日付をArrayListに格納します。
		for (Element tmp : dates) {
			String date = tmp.text();
			if (!date.equals("")) {
				date = date.replaceAll("\\.", "/");
				HTML.add(tmp.text() + " ");
			}
		}
		
		// 日付に件名を追記します。
		int index = 0;
		for (Element tmp : titles) {
			
			String title =HTML.get(index)+tmp.text();
//			HTML.remove(index);
//			HTML.add(index, title);
			HTML.set(index, title);

			
			index++;
		}

		// ArrayListに格納された文字列を一覧で表示します。(テスト用)
		for (int i = 0; i < HTML.size(); i++) {
			resp.getWriter().println(HTML.get(i));
		}

		/**
		 * 
		 * ============ここまで============
		 * 
		 **/

		// データストアのインスタンスを取得します。
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		// キーを生成します。(ここではJsoupというカインドにDocumentというname属性を持ったプライマリーキーを設定します)
		Key key = KeyFactory.createKey(ENTITY_KIND, ENTITY_KEY);
		//		logger.info("key =" + key.toString());

		Entity entity;
		try {
			// データストアからキーに該当するエンティティを取得します。
			entity = datastore.get(key);
		} catch (EntityNotFoundException e) {
			// 初回起動時、エンティティが存在しない場合の処理です。

			// エンティティのインスタンスを取得します。
			entity = new Entity(key);

			// (ここではPre_HTML_Dataというプロパティ(カラム)に
			// 前回取得したHTMLを値として保存します)
			entity.setProperty(ACCESS_KEY_FIELD, currentHTML);
			// データベースにエンティティを保存します。
			datastore.put(entity);

			resp.getWriter().println("初回起動時のため比較するデータがありません");
			return;

		}
		// データストアに保存された前回取得した内容を取得します。
		String preHTML = (String) entity.getProperty(ACCESS_KEY_FIELD);

		//		resp.getWriter().println("前回のデータ:\n");
		//		resp.getWriter().println(preHTML);
		//		resp.getWriter().println("\n");

		//		resp.getWriter().println("最新のデータ:\n");
		//		resp.getWriter().println(currentHTML);
		//		resp.getWriter().println("\n");

		if (currentHTML.equals(preHTML)) {
			//			resp.getWriter().println("変更なし\n");

		} else {

			//			resp.getWriter().println("変更あり\n");
			// トランザクション処理を開始します。
			Transaction txn = datastore.beginTransaction();
			try {

				entity.setProperty(ACCESS_KEY_FIELD, currentHTML);
				datastore.put(entity);
				txn.commit();
			} finally {
				if (txn.isActive()) {
					txn.rollback();
				}
			}
		}

	}
}
