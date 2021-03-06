package io.kurumi.ntt.funcs;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.fragment.Fragment;
import io.kurumi.ntt.model.Msg;
import io.kurumi.ntt.twitter.TAuth;
import io.kurumi.ntt.utils.T;
import java.io.File;
import twitter4j.AsyncTwitter;
import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.TwitterException;
import io.kurumi.ntt.db.*;

public class TwitterDelete extends Fragment {

    public static TwitterDelete INSTANCE = new TwitterDelete();

    final String POINT_DELETE_LIKES = "d|l";

    @Override
    public boolean onPrivate(UserData user,Msg msg) {

        if (msg.doc() == null) return false;

        switch (msg.doc().fileName()) {

            case "like.js" : deleteLikes(user,msg);break;

            default : return false;

        }

        return true;

    }

    @Override
    public boolean onPointedPrivate(UserData user,Msg msg) {

        switch (getPoint(user).point) {

            case POINT_DELETE_LIKES : comfirmDeleteLikes(user,msg);break;

            default : return false;

        }

        return true;

    }

    void deleteLikes(UserData user,Msg msg) {

        if (T.checkUserNonAuth(user,msg)) return;

        msg.send("输入 任意内容 来删除所有的推文喜欢 ","使用 /cancel 取消 注意 : 开始后不可撤销").exec();

        setPoint(user,POINT_DELETE_LIKES,msg.doc().fileId());

    }

    void comfirmDeleteLikes(final UserData user,final Msg msg) {

        try {

            msg.sendTyping();
            
            PointStore.Point<String> point = getPoint(user);

            File likejs = getFile(point.data);

            String content = FileUtil.readUtf8String(likejs);

            content = StrUtil.subAfter(content,"=",false);

            final JSONArray array  = new JSONArray(content);

            msg.send("解析成功 : " + array.length() + "个喜欢 正在处理...").exec();

            final AsyncTwitter api = TAuth.get(user.id).createAsyncApi();

            for (int index = 0;index < array.length();index ++) {
                
                api.destroyFavorite(array.getJSONObject(index).getJSONObject("like").getLong("tweetId"));

            }

            msg.send("已添加到队列 ~").exec();

            clearPoint(user);

        } catch (Exception err) {

            msg.send("解析失败..." + err).exec();

        }


    }

}
