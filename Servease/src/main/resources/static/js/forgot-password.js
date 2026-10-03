const forgotPasswordForm =
    document.getElementById("forgotPasswordForm");

if (forgotPasswordForm) {

    forgotPasswordForm.addEventListener(
        "submit",
        async function (event) {

            event.preventDefault();

            const email =
                document.getElementById("email")
                    .value
                    .trim();

            const message =
                document.getElementById("message");

            const sendOtpButton =
                document.getElementById("sendOtpButton");

            message.className = "message";
            message.textContent = "";

            sendOtpButton.disabled = true;
            sendOtpButton.textContent = "Sending...";

            try {

                const response = await fetch(
                    "/api/auth/forgot-password",
                    {
                        method: "POST",

                        headers: {
                            "Content-Type": "application/json"
                        },

                        body: JSON.stringify({
                            email: email
                        })
                    }
                );

                const data = await response.json();

                if (!response.ok) {

                    throw new Error(
                        data.message ||
                        "Unable to send OTP"
                    );
                }

                message.className =
                    "message success";

                message.textContent =
                    "OTP sent successfully. Check your email.";


                // Save email for the OTP verification page
                sessionStorage.setItem(
                    "resetEmail",
                    email
                );


                // Go to Verify OTP page
                setTimeout(() => {

                    window.location.href =
                        "/verify-otp.html";

                }, 1000);

            } catch (error) {

                message.className =
                    "message error";

                message.textContent =
                    error.message ||
                    "Something went wrong.";

            } finally {

                sendOtpButton.disabled = false;
                sendOtpButton.textContent = "Send OTP";
            }
        }
    );
}