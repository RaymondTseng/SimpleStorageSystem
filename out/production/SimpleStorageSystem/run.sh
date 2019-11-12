java DirectoryServer ../config.txt false &
java DirectoryServer ../config.txt true &
sleep 3
java StorageNode a localhost 8124 ../nodesData/a localhost 8123 localhost 8888 &
java StorageNode b localhost 8125 ../nodesData/b localhost 8123 localhost 8888 &
java StorageNode c localhost 8126 ../nodesData/c localhost 8123 localhost 8888 &
