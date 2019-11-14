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

![](https://github.com/RaymondTseng/SimpleStorageSystem/blob/master/fault_tolerant_storage_system.jpg)


## Manual
#### Run `sh run.sh` in the terminal, it will help us set up the directory servers and nodes in the system. Then, run `Client.java` to get connection with the system:
```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
```
#### We can get the file list from storage node:

```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
1
3.txt
2.txt
1.txt
```
#### We can also get file list from the directory server, which should give us the same file list when the network is running without failure:
```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
2
3.txt
2.txt
1.txt
```
#### If we kill the process of listening on main directory server's port, which means the main server is down, we can still get the file list from the back-up server:
```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
2
use backup
3.txt
2.txt
1.txt
```

#### We can read a file in the storage node:
```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
3
Please enter the file's name
1.txt
1111111111
```
#### We can also add a new file to the system:
```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
4
Please enter the file's name
4.txt
```
```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
1
3.txt
2.txt
1.txt
4.txt
```
```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
2
3.txt
2.txt
1.txt
4.txt
```
#### We can exit the interaction by return `5` in command line.
```Java
5

Process finished with exit code 0
```




>For any question or concern, please contact <ziz54@pitt.edu> or <fag24@pitt.edu>.

