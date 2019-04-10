
package io.kurumi.ntt.twitter.archive;

import cn.hutool.core.convert.NumberChineseFormater;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import io.kurumi.ntt.model.data.IdDataModel;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import twitter4j.MediaEntity;
import twitter4j.Status;
import cn.hutool.core.convert.impl.CalendarConverter;
import java.util.Calendar;
import java.util.*;
import java.time.*;
import io.kurumi.ntt.utils.*;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import io.kurumi.ntt.db.BotDB;

public class StatusArchive {

    public Long id;
    
	public Long createdAt;

	public String text;

	public Long from;

	public Long inReplyToStatusId;

	public String inReplyToScreenName;

    public Long inReplyToUserId;

	public Long quotedStatusId;

	public LinkedList<String> mediaUrls;

    public Boolean isRetweet;

    public Long retweetedStatus;

    public void read(Status status) {

        createdAt = status.getCreatedAt().getTime();
        text = status.getText();

        from = status.getUser().getId();

        UserArchive.saveCache(status.getUser());

        inReplyToStatusId = status.getInReplyToStatusId();

        inReplyToScreenName = status.getInReplyToScreenName();

        inReplyToUserId = status.getInReplyToUserId();

        quotedStatusId = status.getQuotedStatusId();

        if (quotedStatusId != -1 && !BotDB.statusExists(quotedStatusId)) {
            
            BotDB.saveStatus(status.getQuotedStatus());

        }

        mediaUrls = new LinkedList<>();

        for (MediaEntity media : status.getMediaEntities()) {

            mediaUrls.add(media.getMediaURL());

        }

        isRetweet = status.isRetweet();

        if (isRetweet) {

            retweetedStatus = status.getRetweetedStatus().getId();

            if (!BotDB.statusExists(retweetedStatus)) {

                BotDB.saveStatus(status.getRetweetedStatus());

            }

        } else {

            retweetedStatus = -1L;

        }

	}

    public UserArchive getUser() {

        return UserArchive.INSTANCE.get(from);

    }

    public String getURL() {

        return "https://twitter.com/" + getUser().screenName + "/status/" + id;

    }

    public String getHtmlURL() {

        return Html.a(StrUtil.padAfter(text,5,"..."),getURL());

    }

    public String split = "\n\n---------------------\n\n";

    public String toHtml() {

        StringBuilder archive = new StringBuilder();

        if (quotedStatusId != -1) {

            StatusArchive quotedStatus = BotDB.getStatus(quotedStatusId);

            if (quotedStatus == null) {

                archive.append(quotedStatus.toHtml());

            } else {

                archive.append("不可用的推文");

            }

            archive.append(split).append(getUser().getHtmlURL()).append(" 的 ").append(Html.a("回复",getURL()));

        } else if (inReplyToStatusId != -1) {

            StatusArchive inReplyTo = BotDB.getStatus(inReplyToStatusId);

            if (inReplyTo != null) {

                archive.append(inReplyTo.toHtml());

            } else {

                archive.append(notAvilableStatus(inReplyToUserId,inReplyToScreenName));

            }

            archive.append(split).append(getUser().getHtmlURL()).append(" 的 ").append(Html.a("回复",getURL()));


        } else if (isRetweet) {

            StatusArchive retweeted = BotDB.getStatus(retweetedStatus);

            archive.append(getUser().getHtmlURL()).append(" 转推从 " + retweeted.getUser().getHtmlURL()).append(" : ");

            archive.append(retweeted.toHtml());

            return archive.toString();

        } else {

            archive.append(getUser().getHtmlURL()).append(" 的 ").append(Html.a("推文",getURL()));

		}

        String content = text;

        if (!mediaUrls.isEmpty()) {

            content = StrUtil.subBefore(content,"https://t.co",true);

        }


        if (content.startsWith("@")) {

            LinkedList<String> inReplyTo = new LinkedList<>();

            while (content.startsWith("@")) {

                inReplyTo.add(StrUtil.subBefore(content.substring(1)," ",false));

                content = StrUtil.subAfter(content," " ,false);

            }

            Collections.reverse(inReplyTo);

            archive.append(" 给");

            boolean l = false;

            for (String replyTo : inReplyTo) {

                UserArchive user = UserArchive.findByScreenName(replyTo);

                archive.append(" ");

                if (l) archive.append("、");

                if (user != null) {

                    archive.append(user.getHtmlURL());

                } else {

                    archive.append(Html.a("@" + replyTo,"https://twitter.com/" + replyTo));

                } 

                l = true;

            }

        }

        archive.append("\n\n").append(content);

        if (!mediaUrls.isEmpty()) {

            archive.append("\n\n媒体文件 :");

            for (String url : mediaUrls) {

                archive.append(Html.a(" 媒体文件",url));

            }

        }

        archive.append("\n\n在 ");

        Calendar date = Calendar.getInstance();

        date.setTimeInMillis(createdAt);

        archive.append(date.get(Calendar.YEAR) - 2000).append("年").append(date.get(Calendar.MONTH)).append("月").append(date.get(Calendar.DAY_OF_MONTH)).append("日");

        archive.append(", ").append(date.get(Calendar.AM_PM) == 0 ? "上午" : "下午").append(" ").append(date.get(Calendar.HOUR)).append(":").append(date.get(Calendar.MINUTE));

        return archive.toString();

    }

    String notAvilableStatus(Long id,String screenName) {

        if (UserArchive.INSTANCE.exists(id)) {

            return UserArchive.INSTANCE.get(id).getHtmlURL() + " 的 不可用的推文";

        } else {

            return Html.a("@" + screenName,"https://twitter.com/" + screenName) + " 的 不可用的推文";

        }

    }


}
