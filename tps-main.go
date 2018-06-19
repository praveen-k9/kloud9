package main

import (
	"bytes"
	"crypto/tls"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"
)

func test() {
	fmt.Println("I am runnning task.")
}

func taskWithParams(a int, b string) {
	fmt.Println(a, b)
}

func main() {

	if len(os.Args) < 2 {
		fmt.Println(" Oops ! Not received proper arguments, list of " +
			"arguments are {topic Name, Number of Iterations}")
		os.Exit(1)
	} else {

		var NoofIterationsInString = os.Args[2]
		fmt.Println(NoofIterationsInString)
		var NoofIterations, err = strconv.Atoi(NoofIterationsInString)

		if NoofIterations == -1 {
			NoofIterations = 900000
		}

		fmt.Println(NoofIterations)
		fmt.Println(err)
		for i := 0; i < NoofIterations; i++ {

			fmt.Println(time.Now())

			var username string = "admin"
			var passwd string = "XOIknQf3gqXrX0rr"	
			tr := &http.Transport{
				TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
			}

			client := &http.Client{Transport: tr}
			req, err := http.NewRequest("GET", "http://127.0.0.1:8888/api/v1/query?query=http_requests_total", nil)
			//req, err := http.NewRequest("GET", "http://127.0.0.1:8001/logs/containers/istio-citadel-6666869c86-gg2vq_istio-system_citadel-fec989032c4be7fbcb0c1931ce31e3dd192ff9a2081cf5908cf35af7d87678c3.log", nil)
			
			req.SetBasicAuth(username, passwd)
			resp, err := client.Do(req)
			if err != nil {
				log.Fatal(err)	
			}
			bodyText, err := ioutil.ReadAll(resp.Body)

			var byteR = bytes.NewReader(bodyText)

			s := string(bodyText)
			fmt.Println(s)
			fmt.Println(time.Now())
			fmt.Println(resp)

			//resp, err = http.Post("http://localhost/api/v1/ingest/analytics", "application/json", byteR)

			req1, err1 := http.NewRequest("POST", "http://localhost:8088/api/v1/ingest/analytics", byteR)

			req1.Header.Set("content-type", "application/json")
			req1.Header.Set("topic-name", os.Args[1])
			req1.Header.Set("x-auth-header", "abc")
			res, _ := client.Do(req1)

			fmt.Println("******** Result *********\n",res)
			fmt.Println("********* Error occurred********\n",err1)
		}

	}
}
