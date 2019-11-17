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
Considering the potential faults which may happen during all parts of the communication process, we designed the system which is able to handle both directory server and storage node failure. The directory server is holding the address of all nodes, which is crucial information regarding file consistency. So we set up 2 directory server in the system: one main server and one back-up server. The communication is set up with default main server. If the main server is down, the sender won't receive any reply so that it will send the message to the back-up server again. After the broken server is repaired, the working server will send all information it holds to the repaired one to synchronize it.<br>

A storage node could down at any time. Suppose the storage node which connected to the client downs while the client wants to add new file into it or to read file in it, the client will then recognize it is down and send a message to server with the node's information and also to request a new node. The directory server will then delete this node in its node list and assign a new node to client. Since our system is set up with 3 nodes, it can tolerant 2 concurrent storage node failure. Moreover. if we set up the system will `n` nodes, it will be able to tolerant `n - 1` concurrent storage node failure. If the node downs during sending the latest added file to other nodes, there might exist temporary difference between other nodes. When the broken node is repaired, it will again go through the registration process and all nodes will be consistent again.<br>

For example, if `Client 1` is connected to `Node 1`. The client wants to add a new file to node however the node is down, it will not successfully send the message. Then `Client 1` will firstly send a message to the directory server to tell it `Node 1`'s address and port, and then require a new node `Node 2`. After `new file` is successfully added to `Node 2`, the directory will get a message from it and return all node's address and port. Then `Node 2` will send `new file` to all other nodes. The following figure is a flow chart of adding a new file to the system. <br>


![](https://github.com/RaymondTseng/SimpleStorageSystem/blob/master/pics/fault_tolerant_storage_system.jpg)


## Manual
### Normal
#### Run `sh run.sh` in the terminal, it will help us set up the directory servers and nodes in the system. 
```Java
# fguo @ FangzhengdeMacBook-Pro in ~/Desktop/SimpleStorageSystem/src [11:42:08] C:1
$ sh run.sh 
use backup
use backup
Activate DirectoryServer localhost 8100
Activate BackupServer localhost 8101

# fguo @ FangzhengdeMacBook-Pro in ~/Desktop/SimpleStorageSystem/src [11:42:17] 
$ Register 2.txt
Activate c localhost 8002
Activate b localhost 8001
Activate a localhost 8000
Register .DS_Store
Register 1.txt
Register 3.txt
Ready to send: localhost;8002
Ready to send: localhost;8002
Storage node c send file 2.txt to localhost8000
Storage node c send file 2.txt to localhost8001
Storage node a send file .DS_Store to localhost8001
Storage node a send file 1.txt to localhost8001
Storage node a send file .DS_Store to localhost8002
Storage node b send file 3.txt to localhost8002
Storage node a send file 2.txt to localhost8002
Ready to send: localhost;8000
Storage node b send file 3.txt to localhost8000
Storage node a send file 1.txt to localhost8002
Ready to send: localhost;8001
Download 3.txt successfully!
Download .DS_Store successfully!
Ready to send: localhost;8000
Download 1.txt successfully!
Download 2.txt successfully!
Download .DS_Store successfully!
Download 1.txt successfully!
Download 3.txt successfully!
```

#### Then, run `Client.java` to get connection with the system:
```Java
$ java Client            
Connect storage node localhost:8002
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
2.txt
3.txt
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
2.txt
1.txt
3.txt

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
Storage node send: 1.txt
111111

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

### Node Fault

#### We can shut down the node which is connected to the client.
```Java
# fguo @ FangzhengdeMacBook-Pro in ~ [11:52:38] 
$ lsof -i:8000
COMMAND PID USER   FD   TYPE             DEVICE SIZE/OFF NODE NAME
java    801 fguo   10u  IPv6 0x5a4e734da53a8aad      0t0  TCP localhost:49906->localhost:irdmi (CLOSE_WAIT)
java    801 fguo   13u  IPv6 0x5a4e734da53ae62d      0t0  TCP localhost:49908->localhost:irdmi (ESTABLISHED)
java    801 fguo   14u  IPv6 0x5a4e734da53ae06d      0t0  TCP localhost:49909->localhost:irdmi (ESTABLISHED)
java    801 fguo   18u  IPv6 0x5a4e734da53b876d      0t0  TCP localhost:49914->localhost:irdmi (ESTABLISHED)
java    801 fguo   19u  IPv6 0x5a4e734da53b7bed      0t0  TCP localhost:49915->localhost:irdmi (ESTABLISHED)
java    805 fguo    4u  IPv6 0x5a4e734d984c906d      0t0  TCP *:irdmi (LISTEN)
java    805 fguo   10u  IPv6 0x5a4e734da53aebed      0t0  TCP localhost:irdmi->localhost:49908 (ESTABLISHED)
java    805 fguo   11u  IPv6 0x5a4e734da53adaad      0t0  TCP localhost:irdmi->localhost:49909 (ESTABLISHED)
java    805 fguo   12u  IPv6 0x5a4e734da53b81ad      0t0  TCP localhost:irdmi->localhost:49914 (ESTABLISHED)
java    805 fguo   13u  IPv6 0x5a4e734da53b762d      0t0  TCP localhost:irdmi->localhost:49915 (ESTABLISHED)

# fguo @ FangzhengdeMacBook-Pro in ~ [11:55:45] 
$ kill -9 805 
```
#### Following our design, the client will request a new node to connect, and the system still works.
```Java
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
1
Main Server has deleted node localhost;8000
nodelocalhost;8000is dead!
Connect storage node localhost:8002
2.txt
3.txt
1.txt
4.txt
```
### Server Fault
#### We can shut down the main directory server in the system.
```Java
# fguo @ FangzhengdeMacBook-Pro in ~ [11:55:56] 
$ lsof -i:8100
COMMAND  PID USER   FD   TYPE             DEVICE SIZE/OFF NODE NAME
java     801 fguo    6u  IPv6 0x5a4e734d984c9bed      0t0  TCP *:xprint-server (LISTEN)
java     801 fguo    9u  IPv6 0x5a4e734da53c462d      0t0  TCP localhost:xprint-server->localhost:49926 (ESTABLISHED)
java     801 fguo   21u  IPv6 0x5a4e734da53c68ad      0t0  TCP localhost:xprint-server->localhost:49927 (ESTABLISHED)
java     801 fguo   22u  IPv6 0x5a4e734da53c3aad      0t0  TCP localhost:xprint-server->localhost:49928 (ESTABLISHED)
java     801 fguo   23u  IPv6 0x5a4e734da53cbbed      0t0  TCP localhost:xprint-server->localhost:49929 (ESTABLISHED)
java     801 fguo   24u  IPv6 0x5a4e734da53cc1ad      0t0  TCP localhost:xprint-server->localhost:49930 (ESTABLISHED)
java     801 fguo   25u  IPv6 0x5a4e734da53cc76d      0t0  TCP localhost:xprint-server->localhost:49931 (ESTABLISHED)
java     801 fguo   26u  IPv6 0x5a4e734da53ccd2d      0t0  TCP localhost:xprint-server->localhost:49932 (CLOSE_WAIT)
java     801 fguo   27u  IPv6 0x5a4e734da53cd8ad      0t0  TCP localhost:xprint-server->localhost:50688 (CLOSE_WAIT)
java     801 fguo   28u  IPv6 0x5a4e734da53c576d      0t0  TCP localhost:xprint-server->localhost:50946 (CLOSE_WAIT)
java     801 fguo   29u  IPv6 0x5a4e734da53b98ad      0t0  TCP localhost:xprint-server->localhost:51222 (CLOSE_WAIT)
java     801 fguo   32u  IPv6 0x5a4e734da53b6aad      0t0  TCP localhost:xprint-server->localhost:52096 (CLOSE_WAIT)
java     806 fguo    7u  IPv6 0x5a4e734da53a962d      0t0  TCP localhost:49902->localhost:xprint-server (CLOSE_WAIT)
java     806 fguo    9u  IPv6 0x5a4e734da53c34ed      0t0  TCP localhost:49929->localhost:xprint-server (ESTABLISHED)
java     806 fguo   11u  IPv6 0x5a4e734da53c6e6d      0t0  TCP localhost:49930->localhost:xprint-server (ESTABLISHED)
java     806 fguo   12u  IPv6 0x5a4e734da53cb62d      0t0  TCP localhost:49931->localhost:xprint-server (ESTABLISHED)
java     807 fguo    7u  IPv6 0x5a4e734da53a906d      0t0  TCP localhost:49904->localhost:xprint-server (CLOSE_WAIT)
java     807 fguo   11u  IPv6 0x5a4e734da53c406d      0t0  TCP localhost:49928->localhost:xprint-server (ESTABLISHED)
java     807 fguo   13u  IPv6 0x5a4e734da53c51ad      0t0  TCP localhost:49926->localhost:xprint-server (ESTABLISHED)
java     807 fguo   14u  IPv6 0x5a4e734da53c4bed      0t0  TCP localhost:49927->localhost:xprint-server (ESTABLISHED)
java    1225 fguo    6u  IPv6 0x5a4e734d984c84ed      0t0  TCP localhost:52094->localhost:xprint-server (CLOSE_WAIT)

# fguo @ FangzhengdeMacBook-Pro in ~ [12:06:52] 
$ kill -9 801 

```
#### Following our design, messages which are sent to the main server will be resend to the back-up server, the system still works.
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
2.txt
1.txt
3.txt
4.txt
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
4
Please enter the file's name
5.txt
*******************************************************
Enter 1 : Get Files List From Storage Node.
Enter 2 : Get Files List From Directory Server.
Enter 3 : Read File
Enter 4 : Add new file.
Enter 5 : Exit.
*******************************************************
5.txt create successfully!
use backup
Add 5.txt to Directory Server!
Storage node c send file 5.txt to localhost8001
Download 5.txt successfully!
use backup
use backup
use backup
use backup
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
4.txt
5.txt
```

>For any question or concern, please contact <ziz54@pitt.edu> or <fag24@pitt.edu>.

