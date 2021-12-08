package tian.bogdan.mihai.student976188.myapplication

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import tian.bogdan.mihai.student976188.myapplication.databinding.ActivityMainBinding
import tian.bogdan.mihai.student976188.myapplication.ui.logIn.LogInFragment


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_sign_in
            ), drawerLayout
        )

        auth = Firebase.auth

        val navigationView: NavigationView = this.findViewById(R.id.nav_view)
        val menu: Menu = navigationView.menu
        val menuItem: MenuItem? = menu.findItem(R.id.nav_sign_out)
        val menuItem4: MenuItem? = menu.findItem(R.id.nav_dashboard)

        menuItem?.setOnMenuItemClickListener {
            signOutClickListener(menu, navigationView)
        }

        menuItem4?.isVisible = false
        if (auth.currentUser != null && !auth.currentUser!!.isAnonymous){
            enableAuthenticatedUserActions(menu, navigationView)
        }else{
            signInAnonymously()
        }

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun signOutClickListener(menu: Menu, navigationView: NavigationView):Boolean{
        val menuItem1: MenuItem? = menu.findItem(R.id.nav_sign_in)
        menuItem1?.isVisible = true
        navigationView.getHeaderView(0)?.findViewById<TextView>(R.id.username)?.text = "Guest"

        LoginManager.getInstance().logOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut()
        Firebase.auth.signOut()
        val menuItem4: MenuItem? = menu.findItem(R.id.nav_dashboard)
        menuItem4?.isVisible = false
        val menuItem3: MenuItem? = menu.findItem(R.id.nav_sign_out)
        menuItem3?.isVisible = false
        return true
    }

    fun enableAuthenticatedUserActions(menu: Menu, navigationView: NavigationView){
        val menuItem1: MenuItem? = menu.findItem(R.id.nav_sign_in)
        menuItem1?.isVisible = false
        navigationView.getHeaderView(0)?.findViewById<TextView>(R.id.username)?.text = FirebaseAuth.getInstance().currentUser?.email
        val menuItem2: MenuItem? = menu.findItem(R.id.nav_sign_out)
        menuItem2?.isVisible = true
        val menuItem4: MenuItem? = menu.findItem(R.id.nav_dashboard)
        menuItem4?.isVisible = true

    }

    fun setActionBarTitle(title: String?) {
        supportActionBar?.title = title
    }

    fun signInAnonymously(){
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                val logInFragment = LogInFragment()

                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    Log.d("Auth", "signInAnonymously:success")
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Auth", "signInAnonymously:failure", task.exception)
                    Snackbar.make(findViewById(R.id.nav_host_fragment_content_main), "Anonymous Authentication failed.", Snackbar.LENGTH_SHORT).show();
                }
                logInFragment.updateUI(null)
            }
    }
}