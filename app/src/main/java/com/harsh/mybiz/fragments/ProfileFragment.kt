package com.harsh.mybiz.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.api.Distribution.Exemplar
import com.harsh.mybiz.AuthenticationActivity
import com.harsh.mybiz.R
import com.harsh.mybiz.SplashActivity
import com.harsh.mybiz.utilities.Constants
import kotlinx.coroutines.currentCoroutineContext

class ProfileFragment : Fragment() {
    lateinit var tvBusinessName: TextView
    lateinit var tvBusinessEmail: TextView
    lateinit var tvBusinessMonthlySale: TextView
    lateinit var btnChangeBusinessName: Button
    lateinit var btnChangePassword: Button
    lateinit var btnLogout: Button
    lateinit var btnLinkBusiness: Button
    lateinit var btnUnLinkBusiness: Button
    lateinit var bsChangeBusinessName: BottomSheetDialog
    lateinit var bsChangeBusinessNameView: View
    lateinit var etBusinessName: EditText
    lateinit var btnSaveBusinessName: Button
    lateinit var ibClose: ImageButton

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var fragProfile = inflater.inflate(R.layout.fragment_profile, container, false)
        tvBusinessName = fragProfile.findViewById(R.id.tv_business_name)
        tvBusinessEmail = fragProfile.findViewById(R.id.tv_email)
        tvBusinessMonthlySale = fragProfile.findViewById(R.id.tv_monthly_sale)
        btnChangeBusinessName = fragProfile.findViewById(R.id.btn_change_business_name)
        btnChangePassword = fragProfile.findViewById(R.id.btn_change_password)
        btnLogout = fragProfile.findViewById(R.id.btn_logout)
        btnLinkBusiness = fragProfile.findViewById(R.id.btn_link_account)
        btnUnLinkBusiness = fragProfile.findViewById(R.id.btn_unlink_account)

        checkLinkedUsers()

        btnUnLinkBusiness.setOnClickListener {
            var adb : AlertDialog.Builder = AlertDialog.Builder(fragProfile.context, R.style.myAlertDialogTheme)
            adb.setTitle("Unlink?")
            adb.setMessage("Are you sure you want to unlink user?")
            var ad: AlertDialog = adb.create()
            adb.setPositiveButton("YES"){
                    dialog, which ->
                unLinkAccount()
            }
            adb.setNegativeButton("NO"){
                    dialog, which -> ad.dismiss()
            }
            adb.show()
        }

        btnLinkBusiness.setOnClickListener {

            if(btnLinkBusiness.text.startsWith("Switch to")){
                val qsLinked = Constants.fbStore.collection("businesses").document(Constants.fbAuth.currentUser!!.uid).collection("links").get()
                qsLinked.addOnSuccessListener {
                    documents->
                    if(documents.size()>0){
                        val switchEmail = documents.documents.get(0).getString("email")
                        val switchPassword = documents.documents.get(0).getString("password")
                        if (switchEmail != null && switchPassword != null) {
                            Constants.fbAuth.signInWithEmailAndPassword(switchEmail, switchPassword).addOnSuccessListener {
                                fragProfile.context.startActivity(Intent(fragProfile.context, SplashActivity::class.java))
                            }
                        }
                    }
                }
            }else {
                val bsCurrentUser = BottomSheetDialog(fragProfile.context)
                val bsCurrentUserView = LayoutInflater.from(fragProfile.context)
                    .inflate(R.layout.bottom_sheet_current_user_password, null, false)
                bsCurrentUser.setContentView(bsCurrentUserView)

                val etCurrentUserPass =
                    bsCurrentUserView.findViewById<EditText>(R.id.et_bs_current_user_password)
                val btnCurrentUserNext =
                    bsCurrentUserView.findViewById<Button>(R.id.btn_bs_current_user_next)
                val ibBSCurrentUserClose =
                    bsCurrentUserView.findViewById<ImageButton>(R.id.ib_bs_current_user_close)

                btnCurrentUserNext.setOnClickListener {
                    val currentPassword = etCurrentUserPass.text.toString()
                    if (!currentPassword.trim().isEmpty()) {
                        Constants.fbAuth.signInWithEmailAndPassword(
                            Constants.fbAuth.currentUser!!.email.toString(),
                            currentPassword
                        ).addOnSuccessListener {
                            Constants.toastThis(fragProfile.context, "Password correct!")
                            bsCurrentUser.dismiss()
                            val bsLinkUser = BottomSheetDialog(fragProfile.context)
                            val bsLinkUserView = LayoutInflater.from(fragProfile.context)
                                .inflate(R.layout.bottom_sheet_link_user, null, false)
                            bsLinkUser.setContentView(bsLinkUserView)

                            val etLinkUserEmail =
                                bsLinkUserView.findViewById<EditText>(R.id.et_bs_link_user_email)
                            val etLinkUserPassword =
                                bsLinkUserView.findViewById<EditText>(R.id.et_bs_link_user_password)
                            val btnLinkUserLink =
                                bsLinkUserView.findViewById<Button>(R.id.btn_bs_link_user_link)
                            val ibBSLinkUserClose =
                                bsLinkUserView.findViewById<ImageButton>(R.id.ib_bs_link_user_close)

                            btnLinkUserLink.setOnClickListener {
                                val linkEmail = etLinkUserEmail.text.toString()
                                val linkPassword = etLinkUserPassword.text.toString()
                                val currentUser = Constants.fbAuth.currentUser
                                if (!linkEmail.trim().isEmpty() && !linkPassword.trim().isEmpty()) {
                                    Constants.fbAuth.signInWithEmailAndPassword(
                                        linkEmail,
                                        linkPassword
                                    ).addOnSuccessListener {
                                        val linkUser = Constants.fbAuth.currentUser
                                        val hmLinkCurrent = HashMap<String, String>()
                                        hmLinkCurrent.put("email", currentUser!!.email.toString())
                                        hmLinkCurrent.put("password", currentPassword)
                                        hmLinkCurrent.put("uid", currentUser.uid)
                                        Constants.fbStore.collection("businesses")
                                            .document(Constants.fbAuth.currentUser!!.uid)
                                            .collection("links").document().set(hmLinkCurrent)
                                            .addOnSuccessListener {
                                                if (currentUser != null) {
                                                    Constants.fbAuth.updateCurrentUser(currentUser)
                                                        .addOnSuccessListener {
                                                            val hmLinkUser =
                                                                HashMap<String, String>()
                                                            hmLinkUser.put(
                                                                "email",
                                                                linkUser!!.email.toString()
                                                            )
                                                            hmLinkUser.put("password", linkPassword)
                                                            hmLinkUser.put("uid", linkUser.uid)
                                                            Constants.fbStore.collection("businesses")
                                                                .document(Constants.fbAuth.currentUser!!.uid)
                                                                .collection("links").document()
                                                                .set(hmLinkUser)
                                                                .addOnSuccessListener {
                                                                    Constants.toastThis(
                                                                        fragProfile.context,
                                                                        "Accounts linked!"
                                                                    )
                                                                    bsLinkUser.dismiss()
                                                                    checkLinkedUsers()
                                                                }
                                                        }
                                                }
                                            }

                                    }.addOnFailureListener {
                                        Constants.toastThis(
                                            fragProfile.context,
                                            "Email or password incorrect!"
                                        )
                                        bsLinkUser.dismiss()
                                    }
                                }

                            }

                            ibBSLinkUserClose.setOnClickListener {
                                bsLinkUser.dismiss()
                            }

                            bsLinkUser.show()

                        }.addOnFailureListener {
                            Constants.toastThis(fragProfile.context, "Password incorrect!")
                            bsCurrentUser.dismiss()
                        }
                    }
                }

                ibBSCurrentUserClose.setOnClickListener {
                    bsCurrentUser.dismiss()
                }

                bsCurrentUser.show()
            }

        }

        btnChangeBusinessName.setOnClickListener{
            try{

                bsChangeBusinessName = BottomSheetDialog(fragProfile.context)
                bsChangeBusinessNameView = LayoutInflater.from(fragProfile.context).inflate(R.layout.bottom_sheet_edit_business_name, null, false)
                bsChangeBusinessName.setContentView(bsChangeBusinessNameView)
                etBusinessName = bsChangeBusinessNameView.findViewById(R.id.et_bs_edit_business_name)
                btnSaveBusinessName = bsChangeBusinessNameView.findViewById(R.id.btn_bs_edit_save_business_name)
                ibClose = bsChangeBusinessNameView.findViewById(R.id.ib_bs_edit_business_name_close)

                etBusinessName.setText(tvBusinessName.text)
                btnSaveBusinessName.setOnClickListener{
                    if(!etBusinessName.text.toString().trim().isEmpty()){
                        Constants.fbStore.collection("businesses").document(Constants.uID).update("name", etBusinessName.text.toString()).addOnSuccessListener {
                            bsChangeBusinessName.dismiss()
                            Constants.toastThis(fragProfile.context, "Business name updated successfully!")
                            loadData()
                        }
                    }
                }
                ibClose.setOnClickListener{
                    bsChangeBusinessName.dismiss()
                }

                bsChangeBusinessName.show()

            }catch (ex: Exception){}
        }

        btnLogout.setOnClickListener {
            var adb : AlertDialog.Builder = AlertDialog.Builder(fragProfile.context, R.style.myAlertDialogTheme)
            adb.setTitle("Logout?")
            adb.setMessage("Are you sure you want to logout?")
            var ad: AlertDialog = adb.create()
            adb.setPositiveButton("YES"){
                    dialog, which ->
                    Constants.fbAuth.signOut()
                    Constants.uID=""
                    SaleFragment.alExpandableSales.clear()
                    Constants.toastThis(fragProfile.context, "Logged out!")
                    startActivity(Intent(context, SplashActivity::class.java))
                    activity?.finish()
            }
            adb.setNegativeButton("NO"){
                    dialog, which -> ad.dismiss()
            }
            adb.show()
        }
        btnChangePassword.setOnClickListener {
            var adb : AlertDialog.Builder = AlertDialog.Builder(fragProfile.context, R.style.myAlertDialogTheme)
            adb.setTitle("Change password?")
            adb.setMessage("If you want to change the password, a password reset link will be sent to your email and you will be logged out from this account and your linked accounts will also get unlinked too, are you sure you want to process?")
            var ad: AlertDialog = adb.create()
            adb.setPositiveButton("SEND LINK & LOGOUT"){
                    dialog, which ->
                    Constants.fbAuth.sendPasswordResetEmail(tvBusinessEmail.text.toString()).addOnSuccessListener {
                        Constants.fbAuth.signOut()
                        Constants.toastThis(fragProfile.context, "Logged out!")
                        unLinkAccount()
                        startActivity(Intent(context, SplashActivity::class.java))
                        activity?.finish()
                    }
            }
            adb.setNegativeButton("CANCEL"){
                    dialog, which -> ad.dismiss()
            }
            adb.show()
        }

        loadData()
        return fragProfile
    }

    @SuppressLint("SetTextI18n")
    fun loadData(){
        try{

            val qs = Constants.fbStore.collection("businesses").document(Constants.uID).get()
            qs.addOnSuccessListener {
                tvBusinessName.setText(qs.getResult().getString("name"))
                tvBusinessEmail.setText(qs.getResult().getString("email"))
                tvBusinessMonthlySale.setText("This month's sale ₹ ${SaleFragment.monthlySale}")
            }

        }catch (ex: Exception){}
    }

    @SuppressLint("SetTextI18n")
    fun checkLinkedUsers(){
        val qsLink = Constants.fbStore.collection("businesses").document(Constants.fbAuth.uid.toString()).collection("links").get()
        qsLink.addOnSuccessListener {
            documents->
            if(documents.size()>0){
                for(link in documents){
                    val qsBusinessName = Constants.fbStore.collection("businesses").whereEqualTo("email", link.getString("email")).get()
                    qsBusinessName.addOnSuccessListener {
                        documents->
                        if(documents.size()>0){
                            btnLinkBusiness.setText("Switch to ${documents.documents.get(0).getString("name")}")
                            btnUnLinkBusiness.visibility = View.VISIBLE
                            btnUnLinkBusiness.setText("Unlink ${documents.documents.get(0).getString("name")}")
                        }
                    }
                }
            }else{
                btnLinkBusiness.setText("Link another business")
                btnUnLinkBusiness.visibility = View.GONE
            }
        }
    }

    fun unLinkAccount(){
        val qsLinked = Constants.fbStore.collection("businesses").document(Constants.fbAuth.currentUser!!.uid).collection("links").get()
        qsLinked.addOnSuccessListener {
                documents->
            if(documents.size()>0){
                val unLinkEmail = documents.documents.get(0).getString("email")
                val qsUnLink = Constants.fbStore.collection("businesses").whereEqualTo("email", unLinkEmail).get()
                qsUnLink.addOnSuccessListener {
                        unDocs->
                    if(unDocs.size()>0){
                        val qsU = Constants.fbStore.collection("businesses").document(unDocs.documents.get(0).id).collection("links").whereEqualTo("email", Constants.fbAuth.currentUser!!.email).get()
                        qsU.addOnSuccessListener {
                                linkDocs->
                            if(linkDocs.size()>0){
                                Constants.fbStore.collection("businesses").document(unDocs.documents.get(0).id).collection("links").document(linkDocs.documents.get(0).id).delete().addOnSuccessListener {
                                    Constants.fbStore.collection("businesses").document(Constants.fbAuth.currentUser!!.uid).collection("links").document(documents.documents.get(0).id).delete().addOnSuccessListener {
                                        Constants.toastThis(requireContext(), "Accounts unlinked!")
                                        checkLinkedUsers()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}