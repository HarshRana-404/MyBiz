package com.harsh.mybiz.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.harsh.mybiz.AuthenticationActivity
import com.harsh.mybiz.MainActivity
import com.harsh.mybiz.R
import com.harsh.mybiz.utilities.Constants

class RegistrationFragment : Fragment() {
    lateinit var tvAlreadyAUser : TextView
    lateinit var etBusinessName : EditText
    lateinit var etEmail : EditText
    lateinit var etPassword : EditText
    lateinit var etConfirmPassword : EditText
    lateinit var btnRegister : Button

    lateinit var businessName: String
    lateinit var email: String
    lateinit var password: String
    lateinit var confirmPassword: String
    lateinit var hmBusiness : HashMap<String, String>
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragRegistration : View = inflater.inflate(R.layout.fragment_registration, container, false)
        tvAlreadyAUser = fragRegistration.findViewById(R.id.tv_already_a_user)
        tvAlreadyAUser.setOnClickListener(View.OnClickListener {
            AuthenticationActivity.setFragment(0)
        })

        etBusinessName = fragRegistration.findViewById(R.id.et_r_business_name)
        etEmail = fragRegistration.findViewById(R.id.et_r_email)
        etPassword = fragRegistration.findViewById(R.id.et_r_password)
        etConfirmPassword = fragRegistration.findViewById(R.id.et_r_confirm_password)
        btnRegister = fragRegistration.findViewById(R.id.btn_register)

        btnRegister.setOnClickListener(View.OnClickListener {
            if(isValidated()){
                try{
                    Constants.fbAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                        try{
                            hmBusiness = HashMap<String, String>()
                            hmBusiness.put("name", businessName)
                            hmBusiness.put("email", email)
                            hmBusiness.put("uid", Constants.fbAuth.currentUser!!.uid.toString())
                            Constants.fbStore.collection("businesses").document(Constants.fbAuth.currentUser!!.uid.toString()).set(hmBusiness).addOnSuccessListener {
                                Toast.makeText(context, "Registered successfully!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(context, MainActivity::class.java))
                                activity?.finish()
                            }
                        }catch (ex: Exception){
                            Constants.logThis(ex.toString())
                        }
                    }.addOnFailureListener(){
                        Toast.makeText(context, "Registration failed!", Toast.LENGTH_SHORT).show()
                    }
                }catch (ex: Exception){

                }
            }else{
                Toast.makeText(context, "Enter all details correctly!", Toast.LENGTH_SHORT).show()
            }
        })

        return fragRegistration
    }
    fun isValidated(): Boolean{
        businessName = etBusinessName.text.toString().trim()
        email = etEmail.text.toString().trim()
        password = etPassword.text.toString().trim()
        confirmPassword = etConfirmPassword.text.toString().trim()
        if(!businessName.isEmpty() && !email.isEmpty() && !password.isEmpty() && !confirmPassword.isEmpty()){
            if(confirmPassword.equals(password)){
                return true
            }
        }
        return false
    }
}