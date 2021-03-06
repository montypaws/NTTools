package io.kurumi.ntt.twitter.stream;

import cn.hutool.core.util.*;
import cn.hutool.json.*;
import io.kurumi.ntt.db.*;
import io.kurumi.ntt.funcs.*;
import io.kurumi.ntt.model.request.*;
import io.kurumi.ntt.twitter.*;
import io.kurumi.ntt.twitter.archive.*;
import io.kurumi.ntt.twitter.stream.*;
import io.kurumi.ntt.twitter.track.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import twitter4j.*;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

public class SubTask extends StatusAdapter {

	public static JSONArray enable = SData.getJSONArray("data","stream",true);

	public static void save() {

		SData.setJSONArray("data","stream",enable);

	}

    long userId;
    long tid;
    Twitter api;

    public SubTask(long userId,long tid,Twitter api) {

        this.userId = userId;
        this.tid = tid;
        this.api = api;

    }

	public static AtomicBoolean needReset = new AtomicBoolean(true);

    static ExecutorService statusProcessPool = Executors.newFixedThreadPool(3);

    static TimerTask resetTask = new TimerTask() {

        @Override
        public void run() {

            if (needReset.getAndSet(false)) {

                resetStream();

            }

        }

    };



    static Timer timer;

    public static void start() {

        stop();

		timer = new Timer("NTT TwitterStream Task");

        Date start = new Date();

        start.setMinutes(start.getMinutes() + 10);

		timer.schedule(resetTask,start,5 * 60 * 1000);

    }

    public static void stop() {

        if (timer != null) timer.cancel();
        timer = null;

    }

	public static void stop(Long userId) {

		TwitterStream stream = userStream.remove(userId);

		if (stream != null) stream.cleanUp();

	}

	public static void stopAll() {

		TwitterStreamImpl.shutdownAll();

	}

    static HashMap<Long,TwitterStream> userStream = new HashMap<>();

	static HashMap<Long,List<Long>> currentSubs = new HashMap<>();

	static void resetStream() {

		HashMap<Long,List<Long>> newSubs = new HashMap<>();

		synchronized (UTTask.subs) {

			for (Map.Entry<String,Object> sub : UTTask.subs.entrySet()) {

				newSubs.put(Long.parseLong(sub.getKey()),((JSONArray)sub.getValue()).toList(Long.class));

			}

		}


		for (Map.Entry<Long,List<Long>> sub : newSubs.entrySet()) {

			long userId = sub.getKey();

			if (!enable.contains(userId)) continue;
            
			if (currentSubs.containsKey(userId)) {

				if (currentSubs.get(userId).equals(sub.getValue())) {

					continue;

				}

			}

			if (TAuth.exists(userId)) {

				TwitterStream stream = new TwitterStreamFactory(TAuth.get(userId).createConfig()).getInstance();

				stream.addListener(new SubTask(userId,TAuth.get(userId).accountId,TAuth.get(userId).createApi()));

				TwitterStream removed = userStream.put(userId,stream);
				if (removed != null) removed.cleanUp();

				stream.filter(new FilterQuery().follow(ArrayUtil.unWrap(sub.getValue().toArray(new Long[sub.getValue().size()]))));

			}

		}

		currentSubs.clear();
		currentSubs.putAll(newSubs);

	}

    @Override
    public void onStatus(final Status status) {

        statusProcessPool.execute(new Runnable() {

                @Override
                public void run() {

                    processStatus(status,userId,tid,api);

                }

            });

    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

        statusDeletionNotice.getStatusId();
		statusDeletionNotice.getUserId();

    }

    static void processStatus(Status status,Long userId,Long tid,Twitter api) {

        // List<Long> userSub = currentSubs.get(userId);

        if (status.getRetweetedStatus() != null) {

            // 忽略 无关转推 (可能是大量的)

			// return;

        }

		//   if (from == tid) return; // 去掉来自自己的推文？

        StatusArchive archive = BotDB.saveStatus(status).loop(api);

        Send send = new Send(userId,archive.toHtml(0)).html();

		if (archive.inReplyToStatusId != -1 || archive.quotedStatusId != -1) {

			send.buttons(StatusUI.INSTANCE.makeShowButton(status.getId()));

		}

		send.exec();

    }

}
