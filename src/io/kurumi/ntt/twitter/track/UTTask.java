package io.kurumi.ntt.twitter.track;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import io.kurumi.ntt.db.SData;
import io.kurumi.ntt.db.UserData;
import io.kurumi.ntt.model.request.Send;
import io.kurumi.ntt.twitter.TAuth;
import io.kurumi.ntt.twitter.archive.UserArchive;
import io.kurumi.ntt.utils.BotLog;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import io.kurumi.ntt.funcs.HideMe;
import io.kurumi.ntt.db.BotDB;
import io.kurumi.ntt.twitter.stream.SubTask;

public class UTTask extends TimerTask {

    static UTTask INSTANCE = new UTTask();
    static Timer timer;

	static LinkedHashSet<Long> pedding = new LinkedHashSet<>();

    public static void start() {

        stop();

        timer = new Timer("NTT Twitter User Track Task");
        timer.scheduleAtFixedRate(INSTANCE,new Date(),1 * 60 * 1000);

    }

    public static void stop() {

        if (timer != null) timer.cancel();
        timer = null;

    }

    int indexG = 0;
    HashMap<String,Integer> useH = new HashMap<>();

    @Override
    public void run() {

        if (TAuth.auth.isEmpty()) return;

        if (indexG == 15) {

            indexG = 0;

            useH.clear();

        }

        indexG ++;

        HashMap<Long,List<Long>> subIndex = new HashMap<>();
        HashMap<UserArchive,String> changes = new HashMap<>();

		synchronized (pedding) {

			Collection<JSONArray> values = (Collection<JSONArray>)(Object)subs.values();

			for (JSONArray arr : values) {

				pedding.addAll(arr.toList(Long.class));

			}

		}

        boolean finished = false;

        try {

            int index = 0;

            while (!finished) {

                if (index > 850) {

                    // 十五分钟user上限900次

                    return;

                }

                LinkedList<String> names;

                synchronized (TAuth.auth) {

                    names = new LinkedList<String>(TAuth.auth.keySet());

                }
                
                for (String id : names) {

                    if (useH.containsKey(id)) {

                        if (useH.get(id) > index) {

                            continue;

                        }

                        useH.put(id,useH.get(id));

                    } else {

                        useH.put(id,1);

                    }


                    Long userId = Long.parseLong(id);

					for (Map.Entry<String,Object> subl : subs.entrySet()) {

						subIndex.put(Long.parseLong(subl.getKey()),((JSONArray)subl.getValue()).toList(Long.class));

					}



                    Twitter api = TAuth.get(userId).createApi();

					List<Long> globals;
                    List<Long> target;

					synchronized (pedding) {

						globals = new LinkedList<Long>(pedding);

					}

					if (globals.size() > 100) {

                        target = globals.subList(0,100);

                        globals = globals.subList(99,globals.size());


                    } else {

                        target = globals;

                        finished = true;

                    }

					synchronized (pedding) {

						pedding.clear();
						pedding.addAll(globals);

					}

                    try {

						ResponseList<User> result = api.lookupUsers(ArrayUtil.unWrap(target.toArray(new Long[target.size()])));

                        for (User tuser : result) {

                            target.remove(tuser.getId());

                            BotDB.saveUser(tuser);

                        }

                        for (Long da : target) {

                            BotDB.saveUserDisappeared(da);

                        }

                    } catch (TwitterException e) {

                        if (e.getErrorCode() == 17) {

							for (Long da : target) {

								BotDB.saveUserDisappeared(da);

							}

                        } else if (e.getErrorCode() == 89 || e.getErrorCode() == 326) {

							synchronized (pedding) {

								pedding.addAll(target);

							}

							if (finished) finished = false;

							new Send(userId,"对不起！但是乃的认证 " + TAuth.get(userId).getFormatedNameHtml() + " 已无法访问 移除了！ Σ( ﾟω / ").html().exec();

							TAuth.auth.remove(id);

						} else throw e;

					}

				}
            }

            if (changes.isEmpty()) return;

            for (Map.Entry<UserArchive,String> change : changes.entrySet()) {

                UserArchive archive = change.getKey();

                if (!subIndex.containsKey(archive.id)) continue;

                List<Long> subscribers = subIndex.get(archive.id);

                for (Long id : subscribers) {

                    new Send(id,archive.urlHtml(),change.getValue()).html().exec();

                }

            }

        } catch (TwitterException ex) {

            if (ex.getErrorCode() != 130) {

				BotLog.error("UserArchive Failed...",ex);

            }

        }



    }

    public static void onUserChange(UserArchive user,String change) {

		LinkedHashSet<Long> subA = new LinkedHashSet<>();

		LinkedList<Long> subD = new LinkedList<>();

		LinkedList<Long> subL;
		LinkedList<Long> subR;

		synchronized (FTTask.INSTANCE) {

			subL = FTTask.flSubIndex.get(user.id);
			subR = FTTask.frSubIndex.get(user.id);

		}

		if (subL != null) subA.addAll(subL);
		if (subR != null) subA.addAll(subR);

        for (Map.Entry<String,JSONArray> sub : ((Map<String,JSONArray>)(Object)subs).entrySet()) {

            if (sub.getValue().contains(user.id)) {

				long id = Long.parseLong(sub.getKey());

				subA.add(id);
				subD.add(id);

            }

        }

		for (Long sub : subA) {

            if (HideMe.hideList.contains(user.id)) continue;

			if (subL != null && subR != null && subL.contains(sub) && subR.contains(sub)) {

				new Send(sub,"与乃相互关注的 " + user.urlHtml() + " :",change).html().exec();

			} else if (subD.contains(sub)) {

				new Send(sub,"乃订阅的 " + user.urlHtml() + " :",change).html().exec();

			}  else if (subL != null && subL.contains(sub)) {

				new Send(sub,"关注乃的 " + user.urlHtml() + " :",change).html().exec();

			} else if (subR != null && subR.contains(sub)) {

				new Send(sub,"乃关注的 " + user.urlHtml() + " :",change).html().exec();

			}

 		}


    }


    public static JSONObject subs = SData.getJSON("data","subscriptions",true);

    public static boolean exists(UserData user) {

        return subs.containsKey(user.id.toString());

    }

    public static List<Long> get(UserData user) {

        if (subs.containsKey(user.id.toString())) {

            return subs.getJSONArray(user.id.toString()).toList(Long.class);

        }

        return null;

    }

    public static boolean add(UserData user,Long id) {

        synchronized (subs) {

            LinkedHashSet<Long> list = subs.containsKey(user.id.toString()) ? new LinkedHashSet<Long>(subs.getJSONArray(user.id.toString()).toList(Long.class)) : new LinkedHashSet<>();

            boolean result = list.add(id);

            subs.put(user.id.toString(),list);

           // SubTask.needReset.set(true);

            return result;

        }

    }

    public static boolean rem(UserData user,Long id) {

        synchronized (subs) {

            LinkedHashSet<Long> list = subs.containsKey(user.id.toString()) ? new LinkedHashSet<Long>(subs.getJSONArray(user.id.toString()).toList(Long.class)) : new LinkedHashSet<>();

            boolean result = list.remove(id);

            subs.put(user.id.toString(),list);

       //     SubTask.needReset.set(true);

            return result;

        }



    }
	
	public static JSONArray remAll(UserData user) {

        synchronized (subs) {

			//     SubTask.needReset.set(true);

           return (JSONArray)subs.remove(user.id.toString());

        }



    }

    public static void save() {

        SubTask.needReset.set(true);
        
        SData.setJSON("data","subscriptions",subs);

    }



}
