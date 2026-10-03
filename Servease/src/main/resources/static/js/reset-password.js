const form = document.getElementById("resetPasswordForm");

const newPasswordInput =
    document.getElementById("newPassword");

const confirmPasswordInput =
    document.getElementById("confirmNewPassword");

const message =
    document.getElementById("message");

const resetBtn =
    document.getElementById("resetBtn");


// Get email from Verify OTP flow
const email = sessionStorage.getItem("resetEmail");


// If email is missing, user should restart the process
if (!email) {

    window.location.href = "forgot-password.html";
}


// Submit reset password form
form.addEventListener("submit", async function (event) {

    event.preventDefault();


    const newPassword =
        newPasswordInput.value.trim();

    const confirmNewPassword =
        confirmPasswordInput.value.trim();


    // Basic validation
    if (newPassword.length < 6) {

        showMessage(
            "Password must be at least 6 characters long.",
            "error"
        );

        return;
    }


    // Confirm password
    if (newPassword !== confirmNewPassword) {

        showMessage(
            "Passwords do not match.",
            "error"
        );

        return;
    }


    resetBtn.disabled = true;

    resetBtn.textContent = "Resetting...";


    try {

        const response = await fetch(
            "/api/auth/reset-password",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({

                    email: email,

                    newPassword: newPassword,

                    confirmNewPassword: confirmNewPassword

                })
            }
        );


        const data = await response.json();


        if (response.ok && data.success) {

            showMessage(
                "Password reset successfully. Redirecting to login...",
                "success"
            );


            // Remove reset email after successful reset
            sessionStorage.removeItem("resetEmail");


            // Go to login
            setTimeout(() => {

                window.location.href = "login.html";

            }, 1200);


        } else {

            showMessage(
                data.message ||
                "Unable to reset password. Please try again.",
                "error"
            );

        }

    } catch (error) {

        console.error(
            "Reset password error:",
            error
        );


        showMessage(
            "Something went wrong. Please try again later.",
            "error"
        );

    } finally {

        resetBtn.disabled = false;

        resetBtn.textContent = "Reset Password";

    }

});


function showMessage(text, type) {

    message.textContent = text;

    message.className =
        "message " + type;

}