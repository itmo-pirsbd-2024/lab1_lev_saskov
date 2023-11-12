package server;

public class User {
    public String name;
    public int age;

    // геттеры, сеттеры и другие методы

    public User() {
        // Пустой конструктор для десериализации JSON
    }

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

