package com.karthikb351.vitinfo2.api.typeadapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * Created by karthikbalakrishnan on 23/02/15.
 */

public class RefreshTypeAdapterFactory implements TypeAdapterFactory {


    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        return null;
    }

    //    public <T> TypeAdapter<T> create(Gson gson, final TypeToken<T> type) {
//
//        final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
//        final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);
//
//        return new TypeAdapter<T>() {
//
//            public void write(JsonWriter out, T value) throws IOException {
//                delegate.write(out, value);
//            }
//
//            public T read(JsonReader in) throws IOException {
//
//                LoginResponse response = new LoginResponse();
//                JsonElement jsonElement = elementAdapter.read(in);
//
//                if (jsonElement.isJsonObject()) {
//                    JsonObject jsonObject = jsonElement.getAsJsonObject();
//                    if (jsonObject.has("data") && jsonObject.get("data").isJsonObject())
//                    {
//                        jsonElement = jsonObject.get("data");
//                    }
//                }
//
//                return response;
//            }
//        }.nullSafe();
//    }
}