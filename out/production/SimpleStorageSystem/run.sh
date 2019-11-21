java DirectoryServer ../config.txt false &
java DirectoryServer ../config.txt true &
sleep 3
java StorageNode a localhost 8000 ../nodesData/a localhost 8100 localhost 8101 &
java StorageNode b localhost 8001 ../nodesData/b localhost 8100 localhost 8101 &
java StorageNode c localhost 8002 ../nodesData/c localhost 8100 localhost 8101 &
java StorageNode d localhost 8003 ../nodesData/d localhost 8100 localhost 8101 &
java StorageNode e localhost 8004 ../nodesData/e localhost 8100 localhost 8101 &
