package tian.bogdan.mihai.student976188.myapplication.ui.editReview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import tian.bogdan.mihai.student976188.myapplication.MainActivity
import tian.bogdan.mihai.student976188.myapplication.R
import tian.bogdan.mihai.student976188.myapplication.adapter.ReviewAdapter
import tian.bogdan.mihai.student976188.myapplication.databinding.FragmentEditReviewBinding
import tian.bogdan.mihai.student976188.myapplication.model.Review
import tian.bogdan.mihai.student976188.myapplication.ui.restaurantView.RestaurantFragment
import java.io.ByteArrayOutputStream
import java.util.*

class EditReviewFragment:Fragment() {
    private var _binding: FragmentEditReviewBinding? = null
    private val binding get() = _binding!!
    private var photoBitmap: Bitmap? = null
    private var clientLocation: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var  storage: FirebaseStorage


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


                    fusedLocationClient.lastLocation.addOnCompleteListener {
                        if (it.result != null) {
                            clientLocation = it.result
                            val addresses: List<Address>
                            val geocoder = Geocoder(requireContext(), Locale.getDefault())

                            addresses = geocoder.getFromLocation(
                                clientLocation?.latitude!!,
                                clientLocation?.longitude!!,
                                1
                            ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

                            val city: String = addresses[0].locality
                            val country: String = addresses[0].countryName
                            val knownName: String = addresses[0].featureName
                            binding.reviewLocation.text = "$country, $city, $knownName"
                        }
                        Snackbar.make(this.requireView(), "The location has been identified successfully", Snackbar.LENGTH_SHORT).show();

                    }                }
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
    ): View? {

        _binding = FragmentEditReviewBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val db = Firebase.firestore

        val mainActivity: MainActivity = activity as MainActivity
        mainActivity.setActionBarTitle("Edit Review")

        storage = FirebaseStorage.getInstance("gs://swanseacw-329e4.appspot.com")
        binding.reviewText.setText(arguments?.get("description").toString())
        val bitmapFactoryOptions = BitmapFactory.Options()
        if(arguments?.get("photo") != null){
            val bitmap = BitmapFactory.decodeFile(arguments?.get("photo").toString(), bitmapFactoryOptions)
            binding.reviewPhoto.setImageBitmap(bitmap)
            photoBitmap = bitmap
        }
        binding.ratingBar.rating = arguments?.get("rating").toString().toFloat()
        binding.reviewLocation.text = arguments?.get("location").toString()
        if(arguments?.get("latitude") != null){
            clientLocation = Location("")
            clientLocation!!.latitude = arguments?.get("latitude").toString().toDouble()
            clientLocation!!.longitude = arguments?.get("longitude").toString().toDouble()

        }
        // Create adapter passing in the sample user data
        // Attach the adapter to the recyclerview to populate items

        binding.submitReview.setOnClickListener{
            when (it.id){
                R.id.submit_review -> {
                    postReview(binding.reviewInput.editText?.text.toString(),
                        FirebaseAuth.getInstance().currentUser?.email, binding.ratingBar.rating,
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
                    binding.reviewPhoto.setImageBitmap(null)
                    binding.reviewPhoto.setImageBitmap(photoBitmap)
                }
            }
        }

        binding.buttonLoadReviewLoc.setOnClickListener { it ->
            print("test")
            print(it.id)
            when (it.id) {
                R.id.button_load_review_loc -> {
                    getLocation()
                }
            }
        }
        return root
    }


    private fun postReview(reviewText: String, user: String?, rating: Float, restaurantRef: String?){
        val db = Firebase.firestore

        val city = hashMapOf(
            "restaurantRef" to restaurantRef,
            "reviewerName" to user,
            "rating" to rating.toString(),
            "description" to reviewText,
            "reviewPhoto" to "${user}${restaurantRef}.jpg",
            "restaurantName" to  arguments?.get("restaurantName")
        )

        if(clientLocation != null){
            city["latitude"] = clientLocation!!.latitude.toString()
            city["longitude"] = clientLocation!!.longitude.toString()
        }


        if(photoBitmap != null){
            val storageRef = storage.reference
            val imagesRef: StorageReference? = storageRef.child("/${user}${restaurantRef}.jpg")

            val baos = ByteArrayOutputStream()
            photoBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val uploadTask = imagesRef?.putBytes(data)
            uploadTask?.addOnFailureListener {
                Snackbar.make(this.requireView(), "The photo has been uploaded successfully", Snackbar.LENGTH_SHORT).show();

            }?.addOnSuccessListener {
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                // ...
            }
        }


        if (restaurantRef != null) {
            db.collection("reviews").whereEqualTo("restaurantRef", restaurantRef)
                .whereEqualTo("reviewerName", user).get().addOnCompleteListener {
                    Log.d("testprostule", it.toString())
                    db.collection("reviews").document(it.result.documents[0].id).update(city.toMap())

                }
        }

        parentFragmentManager.popBackStack()
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
                        val addresses: List<Address>
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())

                        addresses = geocoder.getFromLocation(
                            clientLocation?.latitude!!,
                            clientLocation?.longitude!!,
                            1
                        ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

                        val city: String = addresses[0].locality
                        val country: String = addresses[0].countryName
                        val knownName: String = addresses[0].featureName

                        binding.reviewLocation.text = "$country, $city, $knownName"
                    }
                    Snackbar.make(this.requireView(), "The location has been identified successfully", Snackbar.LENGTH_SHORT).show();

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
}