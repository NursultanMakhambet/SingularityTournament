import { Formik, Field, ErrorMessage } from "formik";
import { ChakraProvider } from "@chakra-ui/react";
import {
  Box,
  Button,
  Flex,
  FormControl,
  FormLabel,
  FormErrorMessage,
  Input,
  VStack,
} from "@chakra-ui/react";

async function getUser() {
  try {
    const token = sessionStorage.getItem("token");
  } catch (err) {
    console.log(err);
  }
}

export default function Login() {
  return (
    <ChakraProvider>
      <Flex bg="gray.100" align="center" justify="center" h="100vh">
        <Box bg="white" p={6} rounded="md" w={64}>
          <Formik
            initialValues={{
              login: "",
              password: "",
            }}
            onSubmit={async (values) => {
              try {
                const req = await fetch(
                  "http://localhost:8189/api/v1/app/auth",
                  {
                    method: "POST",
                    body: JSON.stringify(values, null, 2),
                    headers: {
                      "Content-Type": "application/json",
                      Accept: "application/json",
                      "Access-Control-Allow-Origin": "*",
                    },
                  }
                );
                const reqJ = await req.json();
                if (reqJ.token) {
                  sessionStorage.setItem("token", reqJ.token);
                  const userReq = await fetch(
                    "http://localhost:8189/api/v1/app/user",
                    {
                      method: "GET",
                      headers: {
                        "Content-Type": "application/json",
                        Accept: "application/json",
                        Authorization: `Bearer ${reqJ.token}`,
                      },
                    }
                  );

                  const userRes = await userReq.json();
                  sessionStorage.setItem(
                    "user",
                    `${userRes.lastName} ${userRes.firstName}`
                  );
                  sessionStorage.setItem("login", `${userRes.login}`);
                  window.location.pathname = "/";
                } else {
                  alert(reqJ.message);
                }
                //
              } catch (error) {
                window.location.pathname = "/500";
                console.log(error);
              }
            }}
          >
            {({ handleSubmit, errors, touched }) => (
              <form onSubmit={handleSubmit}>
                <VStack spacing={4} align="flex-start">
                  {/* <FormControl isInvalid={!!errors.name && touched.name}> */}
                  <FormControl>
                    <FormLabel htmlFor="text">Login</FormLabel>
                    <Field
                      as={Input}
                      id="login"
                      name="login"
                      type="login"
                      variant="filled"
                    />
                  </FormControl>
                  <FormControl
                    isInvalid={!!errors.password && touched.password}
                  >
                    <FormLabel htmlFor="password">Password</FormLabel>
                    <Field
                      as={Input}
                      id="password"
                      name="password"
                      type="password"
                      variant="filled"
                      validate={(value) => {
                        let error;

                        if (value.length < 3) {
                          error = "Password must contain at least 3 characters";
                        }

                        return error;
                      }}
                    />
                    <FormErrorMessage>{errors.password}</FormErrorMessage>
                  </FormControl>
                  <Button type="submit" colorScheme="orange" width="full">
                    Login
                  </Button>
                  <a href="/registration">
                    <Button colorScheme="blue" width="full">
                      Sign up
                    </Button>
                  </a>
                </VStack>
              </form>
            )}
          </Formik>
        </Box>
      </Flex>
    </ChakraProvider>
  );
}
