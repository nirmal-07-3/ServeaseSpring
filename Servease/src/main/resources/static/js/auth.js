const loginForm = document.getElementById("loginForm");

if (loginForm) {

    loginForm.addEventListener("submit", async function (event) {

        event.preventDefault();

        const email =
            document.getElementById("email").value.trim();

        const password =
            document.getElementById("password").value;

        const message =
            document.getElementById("message");

        const loginButton =
            document.getElementById("loginButton");

        message.className = "message";
        message.textContent = "";

        loginButton.disabled = true;
        loginButton.textContent = "Logging in...";

        try {

            const response = await fetch("/api/auth/login", {

                method: "POST",

                headers: {
                    "Content-Type": "application/json"
                },

                body: JSON.stringify({
                    email: email,
                    password: password
                })
            });

            const data = await response.json();

            if (!response.ok) {

                throw new Error(
                    data.message || "Invalid email or password"
                );
            }

            /*
             * Your ApiResponse should contain the actual
             * login response inside data.
             *
             * We keep this flexible because your exact
             * LoginResponse structure may differ.
             */

            const loginData = data.data;

            if (loginData && loginData.token) {

                localStorage.setItem(
                    "servease_token",
                    loginData.token
                );
            }

            message.className = "message success";
            message.textContent =
                "Login successful!";

            setTimeout(() => {

                window.location.href = "/dashboard.html";

            }, 800);

        } catch (error) {

            message.className = "message error";
            message.textContent =
                error.message || "Login failed";

        } finally {

            loginButton.disabled = false;
            loginButton.textContent = "Login";
        }
    });
}