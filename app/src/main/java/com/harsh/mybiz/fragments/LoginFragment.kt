package com.harsh.mybiz.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.harsh.mybiz.AuthenticationActivity
import com.harsh.mybiz.MainActivity
import com.harsh.mybiz.R
import com.harsh.mybiz.utilities.Constants

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
class LoginFragment : Fragment() {
    lateinit var tvNotAUser : TextView
    lateinit var tvForgotPassword : TextView
    lateinit var etEmail : EditText
    lateinit var etPassword : EditText
    lateinit var btnLogin : Button

    lateinit var email: String
    lateinit var password: String
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragLogin : View = inflater.inflate(R.layout.fragment_login, container, false)
        etEmail = fragLogin.findViewById(R.id.et_l_email)
        etPassword = fragLogin.findViewById(R.id.et_l_password)
        btnLogin = fragLogin.findViewById(R.id.btn_login)

        tvNotAUser = fragLogin.findViewById(R.id.tv_not_a_user)
        tvForgotPassword = fragLogin.findViewById(R.id.tv_forgot_password)
        tvNotAUser.setOnClickListener(View.OnClickListener {
            AuthenticationActivity.setFragment(1)
        })

        tvForgotPassword.setOnClickListener {
            if(!etEmail.text.toString().trim().isEmpty()){
                Constants.fbAuth.sendPasswordResetEmail(etEmail.text.toString().trim()).addOnSuccessListener {
                    Constants.toastThis(fragLogin.context, "Password reset link sent to email!")
                }
            }else{
                Constants.toastThis(fragLogin.context, "Enter your email to send password reset link")
            }
        }

        btnLogin.setOnClickListener(View.OnClickListener {
            if(isValidated()){
                try{
                    Constants.fbAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        Constants.uID = Constants.fbAuth.uid.toString()
                        startActivity(Intent(context, MainActivity::class.java))
                        activity?.finish()
                    }.addOnFailureListener(){
                        Toast.makeText(context, "Signed in failed!", Toast.LENGTH_SHORT).show()
                    }
                }catch (ex: Exception){

                }
            }else{
                Toast.makeText(context, "Enter all details!", Toast.LENGTH_SHORT).show()
            }
        })

        return fragLogin
    }
    fun isValidated(): Boolean{
        email = etEmail.text.toString().trim()
        password = etPassword.text.toString().trim()
        if(!email.isEmpty() && !password.isEmpty()){
            return true
        }
        return false
    }
}