package tian.bogdan.mihai.student976188.myapplication.ui.logIn

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.facebook.*
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import tian.bogdan.mihai.student976188.myapplication.R
import tian.bogdan.mihai.student976188.myapplication.databinding.FragmentLogInBinding


class LogInFragment : Fragment() {

    private var _binding: FragmentLogInBinding? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var buttonFacebookLogin: LoginButton
    private var someActivityResultLauncher = registerForActivityResult(
        StartActivityForResult(),
        ActivityResultCallback { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d("Testtttt Google", "firebaseAuthWithGoogle:" + account.id)
                    firebaseAuthWithGoogle(account.idToken!!)
                    binding.errorMessage.text = ""
                    binding.errorMessage.visibility = View.INVISIBLE
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("Testtttt Google", "Google sign in failed", e)
                    binding.errorMessage.text = e.message
                    binding.errorMessage.visibility = View.VISIBLE
                }
            }
        })
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLogInBinding.inflate(inflater, container, false)
        val root: View = binding.root

        callbackManager = CallbackManager.Factory.create()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_idd))
            .requestEmail()
            .build()
        auth = Firebase.auth

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        buttonFacebookLogin =  binding.btnSignInFacebook

        buttonFacebookLogin.setPermissions("email", "public_profile")
        buttonFacebookLogin.registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                Log.d("Facebook", "facebook:onSuccess:$result")
                handleFacebookAccessToken(result.accessToken)
            }

            override fun onCancel() {
                Log.d("Facebook", "facebook:onCancel")
            }

            override fun onError(error: FacebookException) {
                Log.d("Facebook", "facebook:onError", error)
                binding.errorMessage.text = error.message
                binding.errorMessage.visibility = View.VISIBLE
            }
        })

        binding.btnSignInGoogle.setOnClickListener {
            print("test")
            print(it.id)
            when (it.id) {
                R.id.btn_sign_in_google -> {
                    val signInIntent = mGoogleSignInClient.signInIntent
                    someActivityResultLauncher.launch(signInIntent)
                }
            }
        }

        binding.signIn.setOnClickListener{
            when (it.id){
                R.id.sign_in -> {
                    signIn(binding.username.text.toString(), binding.password.text.toString())
                }
            }
        }

        binding.signUp.setOnClickListener{
            when (it.id){
                R.id.sign_up -> {
                    createAccount(binding.username.text.toString(), binding.password.text.toString())
                }
            }
        }
        return root
    }

    override fun onStart() {
        val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        //updateUI(account)
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            reload();
        }
        //updateUI(currentUser);
    }

    private fun createAccount(email: String, password: String) {
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Email login", "createUserWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                    binding.errorMessage.text = ""
                    binding.errorMessage.visibility = View.INVISIBLE
                    Snackbar.make(this.requireView(), "The account has been created successfully", Snackbar.LENGTH_SHORT).show();
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Email login", "createUserWithEmail:failure", task.exception)
                    updateUI(null)
                    binding.errorMessage.text = task.exception?.message
                    binding.errorMessage.visibility = View.VISIBLE
                }
            }
        // [END create_user_with_email]
    }

    private fun signIn(email: String, password: String) {
        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Email login", "signInWithEmail:success")
                    val user = auth.currentUser
                    updateUI(user)
                    binding.errorMessage.text = ""
                    binding.errorMessage.visibility = View.INVISIBLE
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Email login", "signInWithEmail:failure", task.exception)
                    updateUI(null)
                    binding.errorMessage.text = task.exception?.message
                    binding.errorMessage.visibility = View.VISIBLE
                }
            }
        // [END sign_in_with_email]
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Testtttt Google", "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Testtttt Google", "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    // [START auth_with_facebook]
    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d("Facebook", "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("Facebook", "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                    binding.errorMessage.text = ""
                    binding.errorMessage.visibility = View.INVISIBLE
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("Facebook", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this.context, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }
    // [END auth_with_facebook]


    fun updateUI(user: FirebaseUser?) {
        val navigationView: NavigationView? = activity?.findViewById(R.id.nav_view)
        val menu: Menu? = navigationView?.menu
        val menuItem4: MenuItem? = menu?.findItem(R.id.nav_dashboard)
        val menuItem1: MenuItem? = menu?.findItem(R.id.nav_sign_in)
        val menuItem: MenuItem? = menu?.findItem(R.id.nav_sign_out)

        if(user != null){
            parentFragmentManager.popBackStack()

            navigationView?.getHeaderView(0)?.findViewById<TextView>(R.id.username)?.text = user.email
            menuItem?.isVisible = true
            menuItem1?.isVisible = false
            menuItem4?.isVisible = true
        }else{
            menuItem?.isVisible = false
            menuItem1?.isVisible = true
            menuItem4?.isVisible = false
        }
    }

    private fun reload() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}