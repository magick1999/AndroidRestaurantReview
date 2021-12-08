package tian.bogdan.mihai.student976188.myapplication.adapter

import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import tian.bogdan.mihai.student976188.myapplication.R
import tian.bogdan.mihai.student976188.myapplication.model.Review
import tian.bogdan.mihai.student976188.myapplication.ui.dashboard.DashboardFragment


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views


class DashboardItemsAdapter (private val mReviews: List<Review>) : RecyclerView.Adapter<DashboardItemsAdapter.ViewHolder>() {

    private lateinit var mListener : onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int){

        }
    }

    fun setOnItemClickListener(listener: onItemClickListener){

        mListener = listener

    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(itemView: View, listener: onItemClickListener) : RecyclerView.ViewHolder(itemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val nameTextView: MaterialTextView = itemView.findViewById(R.id.reviewer_name)
        val descriptionTextView: MaterialTextView = itemView.findViewById(R.id.review_description)
        val ratingTextView: MaterialTextView = itemView.findViewById(R.id.review_rating)
        val reviewImageView: AppCompatImageView = itemView.findViewById(R.id.review_photo)
        val reviewLocation: MaterialTextView = itemView.findViewById(R.id.review_location)
        val editReviewButton: MaterialButton = itemView.findViewById(R.id.edit_review)
        val restaurantNameTextView: MaterialTextView = itemView.findViewById(R.id.restaurant_name)
        init {
            Log.d("test", "init")
            editReviewButton.setOnClickListener { listener.onItemClick(adapterPosition) }
        }
    }

    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        // Inflate the custom layout
        val contactView = inflater.inflate(R.layout.dashboard_item, parent, false)
        // Return a new holder instance
        return ViewHolder(contactView, mListener)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get the data model based on position
        val review: Review = mReviews[position]
        // Set item views based on your views and data model
        viewHolder.nameTextView.text = review.reviewerName

        if(FirebaseAuth.getInstance().currentUser?.email == review.reviewerName){
            viewHolder.editReviewButton.visibility = MaterialButton.VISIBLE
        }else{
            viewHolder.editReviewButton.visibility = MaterialButton.INVISIBLE
        }


        viewHolder.descriptionTextView.text = review.description
        viewHolder.ratingTextView.text = "★ ${review.rating}"
        val bitmapFactoryOptions = BitmapFactory.Options()
        if(review.photo != null){
            viewHolder.reviewImageView.setImageBitmap(BitmapFactory.decodeFile(review.photo?.absolutePath, bitmapFactoryOptions))
        }
        val textView4 = viewHolder.reviewLocation

        if(review.location != null){
            textView4.text = review.location
        }
        viewHolder.restaurantNameTextView.text = review.restaurantName
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return mReviews.size
    }
}