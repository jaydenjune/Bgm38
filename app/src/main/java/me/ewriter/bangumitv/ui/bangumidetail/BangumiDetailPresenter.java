package me.ewriter.bangumitv.ui.bangumidetail;

import android.text.TextUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.ewriter.bangumitv.api.ApiManager;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by zubin on 2016/9/24.
 */

public class BangumiDetailPresenter implements BangumiDetailContract.Presenter {

    private CompositeSubscription mSubscriptions;
    private BangumiDetailContract.View mDetailView;


    public BangumiDetailPresenter(BangumiDetailContract.View mDetailView) {
        this.mDetailView = mDetailView;

        mDetailView.setPresenter(this);
    }

    @Override
    public void subscribe() {
        mSubscriptions = new CompositeSubscription();
    }

    @Override
    public void unsubscribe() {
        mSubscriptions.clear();
    }

    @Override
    public void requestWebDetail(String subjectId) {
        Subscription subscription = ApiManager.getWebInstance()
//                .getAnimeDetail(subjectId)
                .getAnimeEp(subjectId)
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
//                        return parseAnimeDetail(s);
                        return parseAnimeEP(s);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String s) {

                    }
                });

        mSubscriptions.add(subscription);
    }

    /** 解析网页动画概览 */
    private String parseAnimeDetail(String html) {

        Document document = Jsoup.parse(html);

        // 最顶上的名称， 一般是日文名
        String title = document.select("h1.nameSingle").text();

        // 顶上名字旁边的灰色小字，一般是类别
        String small_type = document.select("h1.nameSingle>small").text();

        // 图片large 地址, 可能为空
        String large_image_url = document.select("div.infobox>div>a").attr("href");

        // 图片cover 地址, 可能为空
        String cover_image_url = document.select("div.infobox>div>a>img").attr("src");

        // 左侧列表
        Elements li = document.select("div.infobox>ul#infobox>li");

        for (int i = 0; i < li.size(); i++) {
            Element element = li.get(i);

            // 左侧的标题，比如 话数：，中文名:
            String left_tip = element.select("span").text();

            // 没有链接的标签 , 有可能是各种 分隔号比如 / 或者 、 还有其它未处理的，如果同时有多个的话要如何显示？
            String left_normal_value = element.ownText();

            // 左侧的的链接地址,不是每个标签都可点,可能有多个
            Elements a = element.select("a");
            for (int j = 0; j < a.size(); j++) {
                Element href = a.get(j);
                String href_name = href.text();
                String href_url = href.attr("href");
            }
        }


        // 右侧收藏盒
        String global_score = document.select("div.global_score").text();

        // 剧情简介
        String summary = document.select("div#subject_summary").text();

        // 标签栏
        Elements tag = document.select("div.subject_tag_section>div.inner>a");
        for (int tagIndex = 0; tagIndex < tag.size(); tagIndex++) {
            Element element = tag.get(tagIndex);

            String tag_name = element.text();
            String tag_src = element.attr("href");
            String abc = "";
        }

        // 角色介绍, 可能不全
        Elements subject_clearit = document.select("ul#browserItemList>li");
        for (int clearItIndex = 0; clearItIndex < subject_clearit.size(); clearItIndex++) {
            Element element = subject_clearit.get(clearItIndex);

            // 角色小头像图片
            String role_image_url = element.select("div>strong>a>span>img").attr("src");

            // 日文名字
            String role_name_jp = element.select("div>strong>a").text();

            // 角色链接
            String role_url = element.select("div>strong>a").attr("href");

            // 角色类型， 主角
            String role_type = element.select("div>div>span>small").text();

            // 角色中文名
            String role_name_cn = element.select("div>div>span>span.tip").text();

            // 声优
            String cv_name = element.select("div>div>span>a").text();

            // 声优链接
            String cv_url = element.select("div>div>span>a").attr("href");

            String a = "";
        }

        // TODO: 2016/9/24   关联条目 ,有好多，暂时没想好怎么放，先不做


        // 吐槽箱
        Elements comments = document.select("div#comment_box>div");
        for (int commentIndex = 0; commentIndex < comments.size(); commentIndex++) {
            Element element = comments.get(commentIndex);

            String userLink = element.select("a").attr("href");
            // TODO: 2016/9/24  style 还需要再处理
            //background-image:url('//lain.bgm.tv/pic/user/s/000/30/28/302862.jpg?r=1472368595')
            String originImage = element.select("a>span").attr("style").replace("\'", "");
            String userAvatar = originImage.substring(originImage.indexOf("(") + 1, originImage.indexOf(")"));
            String userName = element.select("div>a").text();
            String userComment = element.select("div>p").text();
            String commentDate = element.select("div>small").text();
        }

        return "parse_anime_detail";
    }


    /** 解析网页章节 */
    private String parseAnimeEP(String html) {
        Document document = Jsoup.parse(html);

        Elements line_list = document.select("ul.line_list>li");

        int catNumber = document.select("li.cat").size();

        // key 是 cat， value 是 list，list 里面是 item，这里先以 string 代替
        HashMap<String, List<String>> hashMap = new HashMap<>();
        List<String> itemList = new ArrayList<>();

        String key = "";

        for (int i = 0; i < line_list.size(); i++) {
            Element element = line_list.get(i);

            String cat = element.attr("class");

            if (cat.startsWith("line")) {
                // 已放送
                String epAirStatusStr = element.select("h6>span.epAirStatus").attr("title");
                //Air
                String epAirStatus = element.select("h6>span.epAirStatus>span").attr("class");

                String epUrl = element.select("h6>a").attr("href");
                String name_jp = element.select("h6>a").text();
                String name_cn = element.select("h6>span.tip").text();
                String info = element.select("small").text();

                itemList.add(name_cn);
            } else {
                key = element.text();
            }

            // 最后一个
            if (i == line_list.size() -1) {
                hashMap.put(key, itemList);
            } else {
                // 中间
                String nextCat = line_list.get(i + 1).attr("class");
                if (!nextCat.startsWith("line")) {
                    List<String> copyList = new ArrayList<>();
                    copyList.addAll(itemList);
                    hashMap.put(key, copyList);
                    itemList.clear();
                }
            }

        }

        return "parseEP";
    }


    /** 解析网页人物介绍 一页到底，不翻页*/
    private String parseAnimeCharacters(String html) {
        Document document = Jsoup.parse(html);

        Elements elements = document.select("div#columnInSubjectA>div");

        for (int i = 0; i < elements.size(); i++) {

            Element element = elements.get(i);

            String avatar_url = element.select("a").attr("href");
            String avatar_image_url = element.select("a>img").attr("src");

            String role_name_cn = element.select("div.clearit>h2>span").text();
            String role_name_jp = element.select("div.clearit>h2>a").text();

            String info = element.select("div.clearit>div.crt_info").text();

            String cv_url = element.select("div.actorBadge clearit>a").attr("href");
            String cv_image_url = element.select("div.actorBadge clearit>a>img").attr("src");
            String cv_name_jp = element.select("div.actorBadge clearit>p>a").text();
            String cv_name_cn = element.select("div.actorBadge clearit>p>small").text();
        }

        return "parseAnimeCharacters";
    }


    /** 解析网页制作人员 不翻页*/
    private String parseAnimePersons(String html) {

        Document document = Jsoup.parse(html);

        Elements elements = document.select("div#columnInSubjectA>div");

        for (int i = 0; i < elements.size(); i++) {

            Element element = elements.get(i);

            String avatar_url = element.select("a").attr("href");
            String avatar_image_url = element.select("a>img").attr("src");

            String name = element.select("div>h2>a").text();
            String info = element.select("div>div.prsn_info").text();

        }


        return "parseAnimePersons";
    }


    /** 解析网页评论 会翻页*/
    private String parseAnimeComments(String html) {

        Document document = Jsoup.parse(html);

        Elements comments = document.select("div#comment_box>div");

        for (int i = 0; i < comments.size(); i++) {
            Element element = comments.get(i);

            String userLink = element.select("a").attr("href");
            // TODO: 2016/9/24  style 还需要再处理
            //background-image:url('//lain.bgm.tv/pic/user/s/000/30/28/302862.jpg?r=1472368595')
            String originImage = element.select("a>span").attr("style").replace("\'", "");
            String userAvatar = originImage.substring(originImage.indexOf("(") + 1, originImage.indexOf(")"));

            String userName = element.select("div>div>a").text();
            String userComment = element.select("div>div>p").text();
            String commentDate = element.select("div>div>small").text();
        }

        return "parseAnimeComments";
    }

}
