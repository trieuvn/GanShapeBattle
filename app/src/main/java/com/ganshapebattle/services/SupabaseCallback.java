package com.ganshapebattle.services;

// Sử dụng Generics <T> để callback có thể trả về bất kỳ kiểu đối tượng nào
// (ví dụ: List<User>, Gallery, String, ...)
public interface SupabaseCallback<T> {
    void onSuccess(T result);
    void onFailure(Exception e);
}