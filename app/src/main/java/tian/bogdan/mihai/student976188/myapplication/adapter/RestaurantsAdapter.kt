package tian.bogdan.mihai.student976188.myapplication.adapter

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import tian.bogdan.mihai.student976188.myapplication.R
import tian.bogdan.mihai.student976188.myapplication.model.Restaurant


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views


class RestaurantsAdapter (private val mRestaurants: List<Restaurant>) : RecyclerView.Adapter<RestaurantsAdapter.ViewHolder>() {

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
        val nameTextView: MaterialTextView = itemView.findViewById(R.id.restaurant_name)
        val descriptionTextView: MaterialTextView = itemView.findViewById(R.id.restaurant_description)
        val backgroundImage: LinearLayout = itemView.findViewById(R.id.background_image)
        val addressTextView: MaterialTextView = itemView.findViewById(R.id.restaurant_location)
        val ratingTextView: MaterialTextView = itemView.findViewById(R.id.restaurant_rating)
        private val cardView: CardView = itemView.findViewById(R.id.card)
        init {
            Log.d("test", "init")
            cardView.setOnClickListener { listener.onItemClick(adapterPosition) }
        }

    }

    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        // Inflate the custom layout
        val contactView = inflater.inflate(R.layout.card_item, parent, false)
        // Return a new holder instance
        return ViewHolder(contactView, mListener)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get the data model based on position
        val restaurant: Restaurant = mRestaurants[position]
        // Set item views based on your views and data model
        val textView = viewHolder.nameTextView
        textView.text = restaurant.name
        val textView1 = viewHolder.descriptionTextView
        textView1.text = restaurant.description
        val backgroundImage = viewHolder.backgroundImage
        val d = Drawable.createFromPath(restaurant.photo.path)
        backgroundImage.background = d
        val textView2 = viewHolder.addressTextView
        textView2.text = restaurant.location
        val textView3 = viewHolder.ratingTextView
        textView3.text = "★ ${restaurant.rating}"
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return mRestaurants.size
    }
}