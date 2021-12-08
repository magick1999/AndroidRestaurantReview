package tian.bogdan.mihai.student976188.myapplication.ui.restaurantView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import tian.bogdan.mihai.student976188.myapplication.R

class RestaurantViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is slideshow Fragment"
    }

    private val _image = MutableLiveData<Int>().apply {
        value = R.drawable.default_profile
    }
    val text: LiveData<String> = _text

    val avatarResId: LiveData<Int>  = _image
}