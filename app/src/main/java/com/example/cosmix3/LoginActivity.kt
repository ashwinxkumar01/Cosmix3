package com.example.cosmix3

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.cosmix3.MainActivity.Companion.PARTY_ID
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.login_dialog.view.*


class LoginActivity : AppCompatActivity() {

    private fun moveToNextScreen(code: String){
        val nextScreenIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(PARTY_ID, code)
        }
        startActivity(nextScreenIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        createButton.setOnClickListener {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.login_dialog, null)
            //AlertDialogBuilder
            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)
                .setTitle("Create Party")
            //show dialog
            val  mAlertDialog = mBuilder.show()
            //login button click of custom layout
            mDialogView.dialogLoginBtn.setOnClickListener {
                //dismiss dialog
                //get text from EditTexts of custom layout
                val password = mDialogView.dialogPasswEt.text.toString()

                //From here on, this is totally changeable and editable!

                //TODO - Pass code to firebase
                //Show loading Bar
                //If code exists in firebase, then show error
                //Else create new party, move to next screen

                if (AsyncUtils.checkParty(password)){
                    mDialogView.dialogPasswEt.error = "Mix already exists!"
                }
                else{
                    mAlertDialog.dismiss()
                    AsyncUtils.newParty(password)
                    moveToNextScreen(password)
                }
            }
            //cancel button click of custom layout
            mDialogView.dialogCancelBtn.setOnClickListener {
                //dismiss dialog
                mAlertDialog.dismiss()
            }
        }

        joinButton.setOnClickListener {
            val mDialogView = LayoutInflater.from(this).inflate(R.layout.login_dialog, null)
            //AlertDialogBuilder
            val mBuilder = AlertDialog.Builder(this)
                .setView(mDialogView)
                .setTitle("Join Party")
            //show dialog
            val  mAlertDialog = mBuilder.show()
            //login button click of custom layout
            mDialogView.dialogLoginBtn.setOnClickListener {
                //get text from EditTexts of custom layout
                val password = mDialogView.dialogPasswEt.text.toString()

                if (AsyncUtils.checkParty(password)){
                    mAlertDialog.dismiss()
                    moveToNextScreen(password)
                }
                else{
                    mDialogView.dialogPasswEt.error = "Mix doesn't exist!"

                }
            }
            //cancel button click of custom layout
            mDialogView.dialogCancelBtn.setOnClickListener {
                //dismiss dialog
                mAlertDialog.dismiss()

                //TODO - Pass code to firebase
                //Show loading Bar
                //If code exists in firebase, pass code and move to next screen
                //Else showToast


            }
        }
    }
}
