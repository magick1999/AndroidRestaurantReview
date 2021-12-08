package tian.bogdan.mihai.student976188.myapplication.ui.dashboard

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import tian.bogdan.mihai.student976188.myapplication.MainActivity
import tian.bogdan.mihai.student976188.myapplication.R
import tian.bogdan.mihai.student976188.myapplication.adapter.DashboardItemsAdapter
import tian.bogdan.mihai.student976188.myapplication.adapter.ReviewAdapter
import tian.bogdan.mihai.student976188.myapplication.databinding.FragmentDashboardBinding
import tian.bogdan.mihai.student976188.myapplication.model.Review
import tian.bogdan.mihai.student976188.myapplication.ui.editReview.EditReviewFragment
import tian.bogdan.mihai.student976188.myapplication.ui.home.HomeFragment
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class DashboardFragment: Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var  storage: FirebaseStorage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val mainActivity: MainActivity = activity as MainActivity
        mainActivity.setActionBarTitle("Dashboard")

        val recyclerView = binding.myReviews
        val reviews: ArrayList<Review> = ArrayList()

        val manager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = manager

        val adapter = DashboardItemsAdapter(reviews)
        // Attach the adapter to the recyclerview to populate items
        recyclerView.adapter = adapter

        val db = Firebase.firestore

        storage = FirebaseStorage.getInstance("gs://swanseacw-329e4.appspot.com")
        val storageRef = storage.reference
        val reviewsRef = db.collection("reviews")
        reviewsRef.whereEqualTo("reviewerName", FirebaseAuth.getInstance().currentUser?.email).addSnapshotListener { value, e ->
            if (e != null) {
                Log.w("Home Fragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            reviews.clear()

            for (doc in value!!) {
                reviews.add(resultToReview(doc, storageRef))

                adapter.setOnItemClickListener(object : DashboardItemsAdapter.onItemClickListener{
                    override fun onItemClick(position: Int) {

                        editReviewClickListener(reviews, position)
                    }
                })
                adapter.notifyDataSetChanged()
            }
            Log.d("Home Fragment", "Current cites in CA: $reviews")
        }
        return root
    }

    fun resultToReview(document: QueryDocumentSnapshot, storageRef: StorageReference): Review {
        val reviewerName = document.get("reviewerName") as String
        val description = document.get("description") as String
        val rating = document.get("rating") as String
        val restaurantRef = document.get("restaurantRef") as String
        val restaurantName = document.get("restaurantName") as String
        val file = if (document.get("reviewPhoto") != null) intializePhoto(
            storageRef,
            document.get("reviewPhoto") as String
        ) else null

        var location: String? = null
        var latitude: Double? = null
        var longitude: Double? = null
        if (document.get("latitude") != null) {
            val addresses: List<Address>
            val geocoder = Geocoder(requireContext(), Locale.getDefault())

            addresses = geocoder.getFromLocation(
                (document.get("latitude") as String).toDouble(),
                (document.get("longitude") as String).toDouble(),
                1
            ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            val city: String = addresses[0].locality
            val country: String = addresses[0].countryName
            val knownName: String = addresses[0].featureName

            location = "$country, $city, $knownName"
            latitude = document.get("latitude").toString().toDouble()
            longitude = document.get("longitude").toString().toDouble()
        }
        return Review(reviewerName, restaurantRef, restaurantName, description, rating, file, latitude, longitude, location)

    }


    fun intializePhoto(storageRef: StorageReference, photoName: String): File{
        val file: File?
        val pathReference = storageRef.child("/${photoName}")
        file = File("${this.requireContext().filesDir}/photos/${photoName}")
        val homeFragment = HomeFragment()
        homeFragment.writeFileOnInternalStorage(this.requireContext(), file)

        pathReference.getFile(file).addOnSuccessListener {
            // Local temp file has been created
        }.addOnFailureListener {
            // Handle any errors
        }
        return file
    }

    fun editReviewClickListener(reviews: ArrayList<Review>, position: Int){
        Log.d("test", "testt")
        val backStateName: String = this.javaClass.name
        val newFragment: Fragment = EditReviewFragment()
        val bundle = Bundle()
        bundle.putString("message", "proba")
        bundle.putString("restaurantRef", reviews[position].restaurantRef)
        bundle.putString("reviewerName", reviews[position].reviewerName)
        bundle.putString("restaurantName", reviews[position].restaurantName)
        bundle.putString("rating", reviews[position].rating)
        bundle.putString("description", reviews[position].description)
        bundle.putString("photo", reviews[position].photo?.absolutePath)
        if(reviews[position].latitude != null){
            bundle.putDouble("latitude", reviews[position].latitude!!)
            bundle.putDouble("longitude", reviews[position].longitude!!)
            bundle.putString("location", reviews[position].location)
        }

        newFragment.arguments = bundle

        val transaction: FragmentTransaction =
            parentFragmentManager.beginTransaction()
// Replace whatever is in the fragment_container view with this fragment,
// and add the transaction to the back stack if needed
        transaction.replace(R.id.nav_host_fragment_content_main, newFragment)
        transaction.addToBackStack(backStateName)

// Commit the transaction
        transaction.commit()
    }
}