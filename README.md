[![](https://jitpack.io/v/sawolabs/Android-SDK.svg)](https://jitpack.io/#sawolabs/Android-SDK)

## How to Integrate SAWO SDK to an Android app

1. Add following line to root build.gradle repositories block

   ```java
   maven { url 'https://jitpack.io' }
   ```

2. Add this to your app level build.gradle android block
    ```java
       compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
    
        kotlinOptions {
            jvmTarget = '1.8'
        }
    ```

3. Add this to your app level build.gradle dependencies block

   ```java
	   implementation 'com.github.sawolabs:Android-SDK:0.1.8'
   ```

4. Sync your project

5. Go to [https://dev.sawolabs.com](https://dev.sawolabs.com) and create a project and copy the api key and api key secret 

6. Create an Activity in Android Studio to get login success response for this example lets assume it is CallbackActivity

7. In your MainActivity add a button to login and add following code to its onclick handle

   Java

   ```java
   import com.sawolabs.androidsdk.Sawo;
   
   public void onClickLogin(View view) {
       new Sawo(
                   this, 
                   "", // your api key
		   "" // your api key secret
                   ).login(
                   "email", // can be one of 'email' or 'phone_number_sms'
                   CallbackActivity.class.getName()  // Callback class name
           );
   }
   ```

   

   Kotlin

   ```kotlin
   import com.sawolabs.androidsdk.Sawo
   
   fun onClickLogin(view: View) {
           Sawo(
               this,
               "", // your api key
               "" // your api key secret
           ).login(
               "email", // can be one of 'email' or 'phone_number_sms'
               CallbackActivity::class.java.name // Callback class name
           )
       }
   ```

   

8. Get the response payload in the CallbackActivity

   Java

   ```java
   import com.sawolabs.androidsdk.ConstantsKt;
   
   Intent intent = getIntent();
   String message = intent.getStringExtra(ConstantsKt.LOGIN_SUCCESS_MESSAGE);
   
   // continue with your implementation
   ```

   Kotlin

   ```kotlin
   import com.sawolabs.androidsdk.LOGIN_SUCCESS_MESSAGE
   
   val message = intent.getStringExtra(LOGIN_SUCCESS_MESSAGE)
   // continue with your implementation
   ```

9. Recommended: Verify the payload sent by sdk from your backend
   
   We have created a POST request method with the endpoint: 
   ```
   https://api.sawolabs.com/api/v1/userverify/
   ```
   The body of this POST request would have two key-value pairs. The sample is shown below:
	```
	{
	   "user_id" : "a0aca1a0-7460-4e8e-8e46-3baf2c92423d",
	   "verification_token" : "ADdHrvkgi407qNfnAyrIVqokm3OWdKUCdj8y"
	}
	```
	
    Once the user is verified in our server and if the user is valid it would return the following response with response 200:

	```
	{
	   "user_valid": true
	}
 	```
   if the user is not valid it would return  the following response with response 400:
	```
	{
	   "user_valid": true
	}
	```

   if the user is not found it would return  the following response with response 404:
	```
	"User not Found"
	```
