const form = document.getElementById("verifyOtpForm");
const otpInput = document.getElementById("otp");
const message = document.getElementById("message");
const verifyBtn = document.getElementById("verifyBtn");


// Get email saved from Forgot Password page
const email = sessionStorage.getItem("resetEmail");


// If email is missing, go back
if (!email) {
    window.location.href = "forgot-password.html";
}


// Only allow numbers
otpInput.addEventListener("input", function () {

    this.value = this.value.replace(/\D/g, "");

});


form.addEventListener("submit", async function (event) {

    event.preventDefault();

    const otp = otpInput.value.trim();


    // Basic validation
    if (otp.length !== 6) {

        showMessage(
            "Please enter a valid 6-digit OTP.",
            "error"
        );

        return;
    }


    verifyBtn.disabled = true;
    verifyBtn.textContent = "Verifying...";


    try {

        const response = await fetch(
            "/api/auth/verify-otp",
            {
                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    email: email,
                    otp: otp
                })
            }
        );


        const data = await response.json();


        if (response.ok && data.success) {

            showMessage(
                "OTP verified successfully.",
                "success"
            );


            // Move to reset password page
            setTimeout(() => {

                window.location.href = "reset-password.html";

            }, 700);

        } else {

            showMessage(
                data.message || "Invalid OTP. Please try again.",
                "error"
            );

        }

    } catch (error) {

        console.error("OTP verification error:", error);

        showMessage(
            "Something went wrong. Please try again later.",
            "error"
        );

    } finally {

        verifyBtn.disabled = false;
        verifyBtn.textContent = "Verify OTP";

    }

});


function showMessage(text, type) {

    message.textContent = text;

    message.className = "message " + type;

}