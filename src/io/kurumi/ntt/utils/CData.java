package io.kurumi.ntt.utils;

import cn.hutool.json.JSONObject;
import java.io.Serializable;

public class CData extends JSONObject {

    public CData() {
        super();
    }

    public CData(String json) {
        super(json);
    }

    public CData(JSONObject json) {
        super(json);
    }

    public String getPoint() {

        return getStr("p");

    }

    public void setPoint(String point) {

        put("p", point);

    }

    public void setIndex(String point) {

        put("i", point);

    }

    public String getIndex() {

        return getStr("i");

    }

    public CData getData(String key) {

        return new CData(getJSONObject(key));

    }

}

