package com.jpz.go4lunch.models;

import androidx.annotation.Nullable;

import java.util.List;

public class Workmate {

    private String id;
    private String username;
    @Nullable
    private String urlPicture;
    @Nullable
    private String restaurantId;
    @Nullable
    private String restaurantName;
    @Nullable
    private String restaurantAddress;
    @Nullable
    private String restaurantDate;
    @Nullable
    private List<String> restaurantsLikedId;

    /*
    Cloud Firestore will internally convert the objects to supported data types.
    Each custom class must have a public constructor that takes no arguments. In addition,
     the class must include a public getter for each property.
     */

    public Workmate() {

    }

    public Workmate(String id, String username, @Nullable String urlPicture, @Nullable String restaurantId,
                    @Nullable String restaurantName, @Nullable String restaurantAddress,
                    @Nullable String restaurantDate, @Nullable List<String> restaurantsLikedId) {
        this.id = id;
        this.username = username;
        this.urlPicture = urlPicture;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantAddress = restaurantAddress;
        this.restaurantDate = restaurantDate;
        this.restaurantsLikedId = restaurantsLikedId;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    @Nullable
    public String getUrlPicture() {
        return urlPicture;
    }

    @Nullable
    public String getRestaurantId() {
        return restaurantId;
    }

    @Nullable
    public String getRestaurantName() {
        return restaurantName;
    }

    @Nullable
    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    @Nullable
    public String getRestaurantDate() {
        return restaurantDate;
    }

    @Nullable
    public List<String> getRestaurantsLikedId() {
        return restaurantsLikedId;
    }

}
