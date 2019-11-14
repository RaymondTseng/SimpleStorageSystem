# Fault-Tolerant Distributed Storage System
This project is the second course project of CS2510 in Pitt. The aim is to design a distributed file storage system which can tolerant both fault of directory server and storage nodes.<br>
Contributors: [Zihang Zeng](https://github.com/RaymondTseng) and [Fangzheng Guo](https://github.com/toobbby) <br>
## System components
### Directory Server
The directory server is at a well-known location in our network. It stores the locations of all nodes and the file list of the system, maintains the file-consistency of all nodes. When a client request to connect with a node, it will return the location of the first node on list to the client. When a new file has been added to a node, it will receive an adding request from that node and return the node list to that node to ask it send this file to all other nodes. When a new node is added to the system, including the registration process, it will add the file names holding by the new node to its file list, and ask a node to send all files in system to the new node.

### Storage Nodes
Each storage node works as a data replica in the system. After connected to a client, it will respond to the client’s request (gettFileList, readFile, addFile...). When a new file is added, it will notify the directory server and send it to other nodes to maintain the consistency.

### Client
A client can connect to the system by sending an inquiry to the directory server. After getting a node's address and port, it can connect to it to manipulate the system. It can get file list from both the server and the node.

## Fault Tolerance Design
Considering the potential faults which may happen during all parts of the communication process, we designed the system which is able to handle both directory server and storage node failure. The directory server is holding the address of all nodes, which is crucial information regarding file consistency. So we set up 2 directory server in the system: one main server and one back-up server. The communication is set up with defaulty main server. If the main server is down, the sender won't recieve any reply so that it will send the message to the back-up server again. After the broken server is repaired, the working server will send all information it holds to the repaired one to synchronize it.<br>

A storage node could down at anytime. Suppose the storage node which connected to the client downs while the client wants to add new file into it or to read file in it, the client will then recognize it is down and send a message to server with the node's information and also to request a new node. The directory server will then delete this node in its node list, and assign a new node to client. Since our system is set up with 3 nodes, it can tolerant 2 concurrent storage node failure. Moreover. if we set up the system will `n` nodes, it will be able to tolerant `n - 1` concurrent storage node failure. If the node downs during sending the lastest added file to other nodes, there might exist temporary difference between other nodes. When the broken node is repaired, it will again go through the registration process and all nodes will be consistent again.<br>

For example, if `Client 1` is connected to `Node 1`. The client wants to add a new file to node however the node is down, it will not successfully send the message. Then `Client 1` will firstly send a message to the directory server to tell it `Node 1`'s address and port, and then require a new node `Node 2`. After `new file` is successfully added to `Node 2`, the directory will get a message from it and return all node's address and port. Then `Node 2` will send `new file` to all other nodes. The following figure is a flow chart of adding a new file to the system. <br>




### Manual Mode
#### Run `main( )` in `P2PNetwork.java` with no argument, the system will access manual mode. It will firstly go through the registration process for the files currently exist. This process includes the registration of all the files in each Peer's folder on Indexing Server.
```Java
-------------------------------------------------
Activate indexingServer localhost 7123
-------------------------------------------------
-------------------------------------------------
Register lion.txt
Register elephant.txt
Register cat.txt
Activate a localhost 7124
-------------------------------------------------
-------------------------------------------------
Register dog.txt
Register lion.txt
Register giraffe.txt
Activate b localhost 7125
-------------------------------------------------
-------------------------------------------------
Register leopard.txt
Register elephant.txt
Activate c localhost 7126
-------------------------------------------------
-------------------------------------------------
Register horse.txt
Register monkey.txt
Activate d localhost 7127
-------------------------------------------------
-------------------------------------------------
Register tiger.txt
Register dog.txt
Register deer.txt
Activate e localhost 7128
-------------------------------------------------
Register horse.txt Successfully!
Register lion.txt Successfully!
Register elephant.txt Successfully!
Register monkey.txt Successfully!
Register lion.txt Successfully!
Register dog.txt Successfully!
Register deer.txt Successfully!
Register giraffe.txt Successfully!
Register tiger.txt Successfully!
Register dog.txt Successfully!
Register cat.txt Successfully!
Register leopard.txt Successfully!
Register elephant.txt Successfully!

Set up a peer by entering the PEER ID (a, b, c, d, e) :)
```
#### We can select a Peer to operate by its name in the command line.

```Java
Set up a peer by entering the PEER ID (a, b, c, d, e) :)
a
*******************************************************
Enter 1 : Create and register a file.
Enter 2 : Search a file on peers.
Enter 3 : Download file from a peer.
Enter 4 : To exit the program.
*******************************************************
```
#### We can create a new file `a_newfile` in `a`. The new file will be stored in `a`'s folder and automatically registered in indexing server.
```Java
*******************************************************
Enter 1 : Create and register a file.
Enter 2 : Search a file on peers.
Enter 3 : Download file from a peer.
Enter 4 : To exit the program.
*******************************************************
1
Enter the file name: 
a_newfile
Create a_newfile Successfully!
*******************************************************
```
#### We can let `a` search for a certain file on its peers. `a` will send a request with the file name to the indexing server. The indexing server will return the addresses and ports of the peers holding this file to `a`.
```Java
*******************************************************
Enter 1 : Create and register a file.
Enter 2 : Search a file on peers.
Enter 3 : Download file from a peer.
Enter 4 : To exit the program.
*******************************************************
2
Enter the file name: 
dog.txt
Finding dog.txt
File exists in
localhost;7128
localhost;7125
```
#### We can let `a` download a certain file on its peers. This command includes searching for the addresses and the ports of the peers holding this file in indexing server and the downloading process. `a` will send a request to the peers holding the target file and those peers will send the target file back to `a`. The latest downloaded file will be also registered in indexing server.
```Java
*******************************************************
Enter 1 : Create and register a file.
Enter 2 : Search a file on peers.
Enter 3 : Download file from a peer.
Enter 4 : To exit the program.
*******************************************************
3
Enter the file name: 
dog.txt
Finding dog.txt
File exists in
localhost;7125
localhost;7128
Download dog.txt successfully!
```
#### We can exit the interaction by return `4` in command line.
```Java
4

Process finished with exit code 0
```

### Test mode
We create a test mode for this system to test its performance on all kinds of circumstances. We can run `main (M, N, F, C)` in `P2PNetwork` where M represents the number of files, N represents the number of  sequential requests and F represents the time gap between 2 sequential request. For example, we set the argument `M = 50`, `N = 20`, `F = 1000` and `C = 1`. The system is set up with 5 peers originally.



>For any question or concern, please contact <ziz54@pitt.edu> or <fag24@pitt.edu>.

