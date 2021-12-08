package tian.bogdan.mihai.student976188.myapplication.ui.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import tian.bogdan.mihai.student976188.myapplication.MainActivity
import tian.bogdan.mihai.student976188.myapplication.R
import tian.bogdan.mihai.student976188.myapplication.databinding.FragmentHomeBinding
import tian.bogdan.mihai.student976188.myapplication.model.Restaurant
import tian.bogdan.mihai.student976188.myapplication.adapter.RestaurantsAdapter
import tian.bogdan.mihai.student976188.myapplication.ui.restaurantView.RestaurantFragment
import java.io.File
import java.util.*


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    //False means first noun in the name, true means third noun
    private var nameOrRating: Boolean = false
    private var ascendingOrDescending: Boolean = false
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val root: View = binding.root
        val mainActivity: MainActivity = activity as MainActivity
        mainActivity.setActionBarTitle("Home")

        val recyclerView = binding.restaurantsList
        val restaurants: ArrayList<Restaurant> = ArrayList()

        val db = Firebase.firestore

        // Create adapter passing in the sample user data
        val adapter = RestaurantsAdapter(restaurants)
        // Attach the adapter to the recyclerview to populate items
        recyclerView.adapter = adapter
        val storage = FirebaseStorage.getInstance("gs://swanseacw-329e4.appspot.com")
        val storageRef = storage.reference

        val restaurantsRef = db.collection("restaurants1")
        restaurantsRef
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d("Home", "${document.id} => ${document.data}")


                    restaurants.add(documentToRestaurant(document, storageRef))
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Home", "Error getting documents.", exception)
            }

        restaurantsRef.addSnapshotListener { value, e ->
            if (e != null) {
                Log.w("Home Fragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            val cities = ArrayList<String>()
            for (doc in value!!) {
                doc.getString("name")?.let {
                    cities.add(it)
                }
            }
            Log.d("Home Fragment", "Current cites in CA: $cities")
        }

        adapter.setOnItemClickListener(object : RestaurantsAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                Log.d("test", "testt")
                restaurantClickListener(restaurants, position)

            }
        })
        // Set layout manager to position the items
        recyclerView.layoutManager = LinearLayoutManager(activity)
        // That's all!s
//
//        val recyclerView: RecyclerView = binding.recyclerView
//        homeViewModel.text.observe(viewLifecycleOwner, Observer {
//            recyclerView.text = it
//        })

        binding.nameRatingSort.setOnCheckedChangeListener { _, b ->
            nameOrRating = b

            if(b){
                restaurants.forEach {
                    Log.d("test", it.rating)
                }
            }else{
                restaurants.forEach {
                    Log.d("test", it.name)
                }
            }


            sort(restaurants)
            adapter.notifyDataSetChanged()

        }

        binding.ascendingDescendingSort.setOnCheckedChangeListener { _, b ->
            ascendingOrDescending = b
            if(b){
                restaurants.forEach {
                    Log.d("test", it.rating)
                }
            }else{
                restaurants.forEach {
                    Log.d("test", it.name)
                }
            }
            sort(restaurants)
            adapter.notifyDataSetChanged()

        }
        return root
    }

    fun writeFileOnInternalStorage(mcoContext: Context, sFileName: File) {
        val dir = File(mcoContext.filesDir, "photos")
        if (!dir.exists()) {
            dir.mkdir()
        }
        try {
            if (!sFileName.exists()){
                sFileName.createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sort(restaurants: ArrayList<Restaurant>){
        if(!nameOrRating){
            if(!ascendingOrDescending){
                restaurants.sortWith { lhs, rhs -> // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    compareWords(lhs.name, rhs.name)
                }
                //restaurants.sortBy { it.name }
            }else{
                restaurants.sortWith { lhs, rhs -> // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    compareWordsDescending(lhs.name, rhs.name)
                }
            }
        }else{
            if(!ascendingOrDescending){
                restaurants.sortWith { lhs, rhs -> // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    compareRatings(lhs.rating, rhs.rating)
                }
            } else{
                restaurants.sortWith { lhs, rhs -> // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    compareRatingsDescending(lhs.rating, rhs.rating)
                }
            }
        }
    }

    fun compareWords(word1: String, word2: String): Int {

        if (word1 == word2)
            return 0

        word1.forEachIndexed { index, it ->
            if (index > word2.length - 1){
                return 1
            }
            if(it.code < word2[index].code){
                return -1
            }else if (it.code > word2[index].code){
                return 1
            }
        }
        return -1
    }

    fun compareWordsDescending(word1: String, word2: String): Int {

        if (word1 == word2)
            return 0

        word1.forEachIndexed { index, it ->
            if (index > word2.length - 1){
                return -1
            }
            if(it.code < word2[index].code){
                return 1
            }else if (it.code > word2[index].code){
                return -1
            }
        }
        return 1
    }

    fun compareRatings(rating1: String, rating2: String): Int {
        val rating1Flt = rating1.toFloat()
        val rating2Flt = rating2.toFloat()

        if (rating1Flt == rating2Flt)
            return 0

        return if(rating1Flt > rating2Flt)
            1
        else
            -1
    }

    fun compareRatingsDescending(rating1: String, rating2: String): Int {
        val rating1Flt = rating1.toFloat()
        val rating2Flt = rating2.toFloat()

        if (rating1Flt == rating2Flt)
            return 0

        return if(rating1Flt > rating2Flt)
            -1
        else
            1
    }

    fun documentToRestaurant(document: QueryDocumentSnapshot, storageRef: StorageReference): Restaurant{
        val name = document.get("name") as String
        val description = document.get("description") as String
        val location = document.get("location") as String
        val photo = document.getString("photo") as String
        val restaurantRef = document.id
        val pathReference = storageRef.child("/${photo}")
        val file = File("${this.requireContext().filesDir}/photos/${photo}")
        writeFileOnInternalStorage(this.requireContext(), file)

        pathReference.getFile(file).addOnSuccessListener {
            // Local temp file has been created
        }.addOnFailureListener {
            // Handle any errors
        }
        val rating = document.get("rating") as String
        return Restaurant(name, description, file, location, rating, restaurantRef)
    }

    fun restaurantClickListener(restaurants: ArrayList<Restaurant>, position: Int){
        val backStateName: String = this.javaClass.name
        val newFragment: Fragment = RestaurantFragment()
        val bundle = Bundle()
        bundle.putString("message", "proba")
        bundle.putString("restaurantRef", restaurants[position].restaurantRef)
        bundle.putString("restaurantName", restaurants[position].name)
        bundle.putString("image", restaurants[position].photo.path)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}