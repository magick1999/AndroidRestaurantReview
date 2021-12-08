package tian.bogdan.mihai.student976188.myapplication.model

import android.location.Location
import com.google.firebase.firestore.GeoPoint
import java.io.File

public class Review(
    var reviewerName: String,
    val restaurantRef: String,
    val restaurantName: String,
    var description: String,
    var rating: String,
    var photo: File?,
    var latitude: Double?,
    var longitude: Double?,
    var location: String?,
    ) {
}