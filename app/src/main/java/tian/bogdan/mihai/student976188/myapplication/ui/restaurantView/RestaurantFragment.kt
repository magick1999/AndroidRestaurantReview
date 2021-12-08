package tian.bogdan.mihai.student976188.myapplication.ui.restaurantView

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import tian.bogdan.mihai.student976188.myapplication.MainActivity
import tian.bogdan.mihai.student976188.myapplication.R
import tian.bogdan.mihai.student976188.myapplication.databinding.FragmentRestaurantViewBinding
import tian.bogdan.mihai.student976188.myapplication.model.Review
import tian.bogdan.mihai.student976188.myapplication.adapter.ReviewAdapter
import tian.bogdan.mihai.student976188.myapplication.ui.editReview.EditReviewFragment
import tian.bogdan.mihai.student976188.myapplication.ui.home.HomeFragment
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Thread.sleep
import java.util.*
import kotlin.collections.ArrayList


class RestaurantFragment : Fragment() {

    private lateinit var restaurantViewModel: RestaurantViewModel
    private var _binding: FragmentRestaurantViewBinding? = null
    private lateinit var  storage: FirebaseStorage
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var photoBitmap: Bitmap? = null
    private var clientLocation: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private var someActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                photoBitmap = result.data?.extras?.get("data") as Bitmap
                // String picturePath contains the path of selected Image
            }
        })


    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                val pickPicktureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                someActivityResultLauncher.launch(pickPicktureIntent)
                Snackbar.make(this.requireView(), "The photo has been uploaded successfully", Snackbar.LENGTH_SHORT).show();
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    @SuppressLint("MissingPermission")
    val requestPermissionLauncherLocation =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(requireActivity())


                    Log.d("test", requireActivity().toString())
                    Log.d("test", requireContext().toString())
                    Log.d("test", isAdded.toString())
                    Log.d("test", isDetached.toString())
                    Log.d("test", fusedLocationClient.toString())
                    Log.d("test", fusedLocationClient.lastLocation.toString())


                    fusedLocationClient.lastLocation.addOnCompleteListener {
                        if (it.result != null) {
                            clientLocation = it.result
                            Snackbar.make(this.requireView(), "The location has been identified successfully", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                    Snackbar.make(this.requireView(), "The location has been identified successfully", Snackbar.LENGTH_SHORT).show();
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                } else -> {
                // No location access granted.
            }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("testt",arguments?.get("message").toString())
        restaurantViewModel =
            ViewModelProvider(this)[RestaurantViewModel::class.java]

        _binding = FragmentRestaurantViewBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val imageView: ImageView = binding.restaurantPhoto
        restaurantViewModel.avatarResId.observe(viewLifecycleOwner, Observer {
            val d = Drawable.createFromPath(arguments?.get("image") as String?)

            imageView.background = d
        })
        photoBitmap = null
        clientLocation = null
        val recyclerView = binding.reviewsList
        val db = Firebase.firestore

        val mainActivity: MainActivity = activity as MainActivity
        mainActivity.setActionBarTitle(arguments?.get("restaurantName").toString())

        val reviews: ArrayList<Review> = ArrayList()
        // Create adapter passing in the sample user data
        val adapter = ReviewAdapter(reviews)
        // Attach the adapter to the recyclerview to populate items
        recyclerView.adapter = adapter

        storage = FirebaseStorage.getInstance("gs://swanseacw-329e4.appspot.com")
        val storageRef = storage.reference
        val reviewsRef = db.collection("reviews")
        reviewsRef.whereEqualTo("restaurantRef", arguments?.get("restaurantRef"))
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w("Home Fragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                reviews.clear()
                for (doc in value!!) {
                    reviews.add(resultToReview(doc, storageRef))

                    adapter.setOnItemClickListener(object : ReviewAdapter.onItemClickListener{
                        override fun onItemClick(position: Int) {
                            editReviewClickListener(reviews, position)
                        }
                    })
                }
                adapter.notifyDataSetChanged()

            }

        reviewsRef.whereEqualTo("restaurantRef", arguments?.get("restaurantRef")).addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.w("Restaurant Fragment", "listen:error", e)
                return@addSnapshotListener
            }

            var rating = 0.0

            for (dc in snapshots?.documents!!) {
                val currentRating = dc.get("rating").toString().toFloat()
                rating += currentRating
            }
            rating /= snapshots.documents.size

            db.collection("restaurants1")
                .document(arguments?.get("restaurantRef") as String)
                .update("rating", rating.toString())
                .addOnSuccessListener { Log.d("Restaurant Fragment", "DocumentSnapshot successfully updated!") }
                .addOnFailureListener { e -> Log.w("Restaurant Fragment", "Error updating document", e) }

        }

        recyclerView.layoutManager = LinearLayoutManager(activity)
        val user = FirebaseAuth.getInstance().currentUser

        displayInput(user)

        binding.buttonSubmitReview.setOnClickListener{
            when (it.id){
                R.id.button_submit_review -> {
                    postReview(binding.reviewInput.editText?.text.toString(), user?.email, binding.ratingBar.rating,
                        arguments?.get("restaurantRef") as String?
                    )
                }
            }
        }

        binding.buttonLoadPicture.setOnClickListener {
            print("test")
            print(it.id)
            when (it.id) {
                R.id.button_load_picture -> {
                    getCameraPhoto()
                }
            }
        }

        binding.buttonAddLocation.setOnClickListener { it ->
            print("test")
            print(it.id)
            when (it.id) {
                R.id.button_add_location -> {
                    getLocation()
                }
            }
        }

        return root
    }

    private fun displayInput(user: FirebaseUser?){
        if(user != null && !user.isAnonymous){
            binding.leaveUsAReview.visibility = TextView.VISIBLE
            binding.reviewBox.visibility = LinearLayout.VISIBLE
            binding.ratingBar.visibility = RatingBar.VISIBLE
            binding.ratingBar.numStars = 5
        }
        else{
            binding.leaveUsAReview.visibility = TextView.GONE
            binding.reviewBox.visibility = LinearLayout.GONE
            binding.ratingBar.visibility = RatingBar.GONE
        }
    }

    private fun postReview(reviewText: String, user: String?, rating: Float, restaurantRef: String?){
        val db = Firebase.firestore

        val city = hashMapOf(
            "restaurantRef" to restaurantRef,
            "reviewerName" to user,
            "rating" to rating.toString(),
            "description" to reviewText,
            "reviewPhoto" to "${user}${restaurantRef}.jpg",
            "restaurantName" to arguments?.get("restaurantName")
        )

        if(clientLocation != null){
            city["latitude"] = clientLocation!!.latitude.toString()
            city["longitude"] = clientLocation!!.longitude.toString()
        }


        if(photoBitmap != null){
            val storageRef = storage.reference
            val imagesRef: StorageReference = storageRef.child("/${user}${restaurantRef}.jpg")

            val baos = ByteArrayOutputStream()
            photoBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val uploadTask = imagesRef.putBytes(data)
            uploadTask.addOnFailureListener {
                Snackbar.make(this.requireView(), "The photo has been uploaded successfully", Snackbar.LENGTH_SHORT).show();

            }.addOnSuccessListener {
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                // ...
            }
        }


        if (restaurantRef != null) {
            db.collection("reviews").document()
                .set(city)
                .addOnSuccessListener {
                    Snackbar.make(this.requireView(), "The review has been uploaded successfully", Snackbar.LENGTH_SHORT).show(); }
                .addOnFailureListener { e -> Log.w("Restaurant Fragment", "Error writing document", e) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun getLocation(){
        val permission: String = Manifest.permission.ACCESS_FINE_LOCATION
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(requireActivity())


                fusedLocationClient.lastLocation.addOnCompleteListener {
                    if (it.result != null) {
                            clientLocation = it.result
                        Snackbar.make(this.requireView(), "The location has been identified successfully", Snackbar.LENGTH_SHORT).show();
                    }
                }
            }else -> {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.

// ...

// Before you perform the actual permission request, check whether your app
// already has the permissions, and whether your app needs to show a permission
// rationale dialog. For more details, see Request permissions.
            requestPermissionLauncherLocation.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))

        }
        }
    }

    fun getCameraPhoto(){
        val permission: String = Manifest.permission.CAMERA

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                val pickPicktureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                someActivityResultLauncher.launch(pickPicktureIntent)
                Snackbar.make(this.requireView(), "The photo has been uploaded successfully", Snackbar.LENGTH_SHORT).show();

            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    permission)
            }
        }
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
            val thoroughfare: String = addresses[0].thoroughfare
            val knownName: String = addresses[0].featureName

            location = "$country, $city, $thoroughfare, $knownName"
            latitude = document.get("latitude").toString().toDouble()
            longitude = document.get("longitude").toString().toDouble()
        }
        return Review(reviewerName, restaurantRef, restaurantName, description, rating, file,latitude, longitude, location)

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
        bundle.putString("rating", reviews[position].rating)
        bundle.putString("restaurantName", reviews[position].restaurantName)
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