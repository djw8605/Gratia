-- 
-- Bad CPU days
-- +-----------+------------+--------+-----------+---------+
-- | SiteName  | period     | Jobs   | CpuHrs    | WallHrs |
-- +-----------+------------+--------+-----------+---------+
-- | OSG-XSEDE | 2013-07-09 | 137148 |   1908326 |  164410 | 
-- | OSG-XSEDE | 2013-07-12 | 129674 |   3066393 |  106092 | 
-- | OSG-XSEDE | 2013-07-13 | 150915 |   1879898 |  109188 | 
-- | OSG-XSEDE | 2013-07-15 |  62730 |   3076088 |  113480 | 
-- | OSG-XSEDE | 2013-07-18 |  53154 |   1852469 |   77807 | 
-- | OSG-XSEDE | 2013-07-19 |  55741 |   1257152 |   75378 | 
-- | OSG-XSEDE | 2013-07-20 |  63722 |   1839666 |   52033 | 
-- | OSG-XSEDE | 2013-07-21 |  20752 |    647268 |   48370 | 
-- | OSG-XSEDE | 2013-07-22 |  45797 |   1848754 |   74717 | 
-- | OSG-XSEDE | 2013-07-24 |  35104 |    650854 |   69905 | 
-- | OSG-XSEDE | 2013-07-25 |  70577 |   1241105 |   59477 | 
-- | OSG-XSEDE | 2013-08-15 |  47149 |   1299648 |  171756 | 
-- | OSG-XSEDE | 2013-08-16 |  35540 |    686954 |  146946 | 
-- | OSG-XSEDE | 2013-08-18 |  81752 |    755885 |  224098 | 
-- | OSG-XSEDE | 2013-08-23 | 135298 |   8495413 |  170427 | 
-- | OSG-XSEDE | 2013-08-24 |  43611 |    630926 |   47811 | 
-- | OSG-XSEDE | 2013-08-26 | 103506 |   3677704 |  117188 | 
-- | OSG-XSEDE | 2013-08-27 | 111193 |   4264007 |  117469 | 
-- | OSG-XSEDE | 2013-09-04 |  77426 |  10231584 |  126562 | 
-- | OSG-XSEDE | 2013-09-06 |  49438 |   5171047 |   62548 | 
-- | OSG-XSEDE | 2013-09-07 |  80830 |   5191152 |   83420 | 
-- | OSG-XSEDE | 2013-09-09 | 100480 |  10360711 |  161958 | 
-- | OSG-XSEDE | 2013-09-10 |  94280 |  20614694 |  160403 | 
-- | OSG-XSEDE | 2013-09-11 | 122358 | 123095660 |  175193 | 
-- | OSG-XSEDE | 2013-09-12 | 103270 |  56471212 |  139879 | 
-- | OSG-XSEDE | 2013-09-13 |  87148 |   5224899 |  130838 | 
-- ---------------------------------------------------------
--
-- Verification query:
-- SELECT SiteName
--  ,date_format(EndTime, "%Y-%m-%d") as period
--  ,sum(Njobs) as Jobs
--  ,round(sum(CpuUserDuration + CpuSystemDuration)/3600,0) as CpuHrs
--  ,round(sum(WallDuration)/3600,0) as WallHrs
-- FROM VOProbeSummary vps, Site s, Probe p
-- WHERE vps.ProbeName = 'condor:osg-xsede.grid.iu.edu'
-- AND vps.EndTime in ( '2013-07-09 00:00:00' ,'2013-07-12 00:00:00', '2013-07-13 00:00:00'
-- ,'2013-07-15 00:00:00', '2013-07-18 00:00:00' ,'2013-07-19 00:00:00', '2013-07-20 00:00:00'
-- ,'2013-07-21 00:00:00', '2013-07-22 00:00:00' ,'2013-07-24 00:00:00', '2013-07-25 00:00:00'
-- ,'2013-08-15 00:00:00', '2013-08-16 00:00:00' ,'2013-08-18 00:00:00', '2013-08-23 00:00:00'
-- ,'2013-08-24 00:00:00', '2013-08-26 00:00:00' ,'2013-08-27 00:00:00', '2013-09-04 00:00:00'
-- ,'2013-09-06 00:00:00', '2013-09-07 00:00:00' ,'2013-09-09 00:00:00', '2013-09-10 00:00:00'
-- ,'2013-09-11 00:00:00', '2013-09-12 00:00:00' ,'2013-09-13 00:00:00')
-- AND vps.ProbeName  = p.probename AND p.siteid = s.siteid
-- GROUP by SiteName ,period
-- ;


update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 23919 , WallDuration= 35292  where dbid= 1089279874 ;
call del_JUR_from_summary(1089279874);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 554 , WallDuration= 1091  where dbid= 1089299253 ;
call del_JUR_from_summary(1089299253);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1002 , WallDuration= 1899  where dbid= 1089352046 ;
call del_JUR_from_summary(1089352046);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 6905 , WallDuration= 19409  where dbid= 1090814001 ;
call del_JUR_from_summary(1090814001);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 7503 , WallDuration= 13388  where dbid= 1090822335 ;
call del_JUR_from_summary(1090822335);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 601 , WallDuration= 1800  where dbid= 1090905196 ;
call del_JUR_from_summary(1090905196);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 21496 , WallDuration= 23837  where dbid= 1091059386 ;
call del_JUR_from_summary(1091059386);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 132 , WallDuration= 1421  where dbid= 1091189020 ;
call del_JUR_from_summary(1091189020);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 11714 , WallDuration= 15388  where dbid= 1091551775 ;
call del_JUR_from_summary(1091551775);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 6542 , WallDuration= 9341  where dbid= 1091786891 ;
call del_JUR_from_summary(1091786891);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 15664 , WallDuration= 23280  where dbid= 1092004288 ;
call del_JUR_from_summary(1092004288);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 467 , WallDuration= 1916  where dbid= 1092765629 ;
call del_JUR_from_summary(1092765629);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 951 , WallDuration= 1679  where dbid= 1092839745 ;
call del_JUR_from_summary(1092839745);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 373 , WallDuration= 1257  where dbid= 1092872286 ;
call del_JUR_from_summary(1092872286);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 3347 , WallDuration= 6939  where dbid= 1092964782 ;
call del_JUR_from_summary(1092964782);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1447 , WallDuration= 2367  where dbid= 1093083699 ;
call del_JUR_from_summary(1093083699);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2500 , WallDuration= 3780  where dbid= 1094762718 ;
call del_JUR_from_summary(1094762718);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 626 , WallDuration= 1290  where dbid= 1094920789 ;
call del_JUR_from_summary(1094920789);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 6050 , WallDuration= 8692  where dbid= 1095277845 ;
call del_JUR_from_summary(1095277845);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 15225 , WallDuration= 17468  where dbid= 1095782959 ;
call del_JUR_from_summary(1095782959);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 519 , WallDuration= 2486  where dbid= 1095922531 ;
call del_JUR_from_summary(1095922531);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 771 , WallDuration= 2825  where dbid= 1096239592 ;
call del_JUR_from_summary(1096239592);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 789 , WallDuration= 3199  where dbid= 1096240402 ;
call del_JUR_from_summary(1096240402);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 255 , WallDuration= 2104  where dbid= 1096568213 ;
call del_JUR_from_summary(1096568213);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1940 , WallDuration= 2747  where dbid= 1096922397 ;
call del_JUR_from_summary(1096922397);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1324 , WallDuration= 2377  where dbid= 1097712537 ;
call del_JUR_from_summary(1097712537);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 21843 , WallDuration= 25282  where dbid= 1098223882 ;
call del_JUR_from_summary(1098223882);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1716 , WallDuration= 2739  where dbid= 1098241095 ;
call del_JUR_from_summary(1098241095);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 29422 , WallDuration= 32641  where dbid= 1099554482 ;
call del_JUR_from_summary(1099554482);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 3011 , WallDuration= 8784  where dbid= 1099762847 ;
call del_JUR_from_summary(1099762847);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2645 , WallDuration= 3757  where dbid= 1100150800 ;
call del_JUR_from_summary(1100150800);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1323 , WallDuration= 3668  where dbid= 1117278488 ;
call del_JUR_from_summary(1117278488);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 8825 , WallDuration= 10910  where dbid= 1117358679 ;
call del_JUR_from_summary(1117358679);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 14628 , WallDuration= 18115  where dbid= 1117378513 ;
call del_JUR_from_summary(1117378513);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1130 , WallDuration= 3684  where dbid= 1118976814 ;
call del_JUR_from_summary(1118976814);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2678 , WallDuration= 6200  where dbid= 1122309305 ;
call del_JUR_from_summary(1122309305);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 4005 , WallDuration= 6538  where dbid= 1122314091 ;
call del_JUR_from_summary(1122314091);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2728 , WallDuration= 5328  where dbid= 1122347621 ;
call del_JUR_from_summary(1122347621);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1433 , WallDuration= 4192  where dbid= 1122396418 ;
call del_JUR_from_summary(1122396418);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2281 , WallDuration= 4729  where dbid= 1122538410 ;
call del_JUR_from_summary(1122538410);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2724 , WallDuration= 4781  where dbid= 1122540399 ;
call del_JUR_from_summary(1122540399);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2369 , WallDuration= 4574  where dbid= 1122598890 ;
call del_JUR_from_summary(1122598890);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2690 , WallDuration= 11537  where dbid= 1122731208 ;
call del_JUR_from_summary(1122731208);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2438 , WallDuration= 7039  where dbid= 1122731506 ;
call del_JUR_from_summary(1122731506);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1699 , WallDuration= 10533  where dbid= 1122889854 ;
call del_JUR_from_summary(1122889854);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1583 , WallDuration= 6099  where dbid= 1122912100 ;
call del_JUR_from_summary(1122912100);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2306 , WallDuration= 13336  where dbid= 1122969684 ;
call del_JUR_from_summary(1122969684);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 3118 , WallDuration= 5712  where dbid= 1122983988 ;
call del_JUR_from_summary(1122983988);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2910 , WallDuration= 5524  where dbid= 1122984250 ;
call del_JUR_from_summary(1122984250);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1137 , WallDuration= 4804  where dbid= 1123741836 ;
call del_JUR_from_summary(1123741836);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1244 , WallDuration= 3015  where dbid= 1124616022 ;
call del_JUR_from_summary(1124616022);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2986 , WallDuration= 4889  where dbid= 1124705917 ;
call del_JUR_from_summary(1124705917);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1646 , WallDuration= 3758  where dbid= 1124750274 ;
call del_JUR_from_summary(1124750274);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2284 , WallDuration= 4400  where dbid= 1124770474 ;
call del_JUR_from_summary(1124770474);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1750 , WallDuration= 4039  where dbid= 1124956769 ;
call del_JUR_from_summary(1124956769);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 951 , WallDuration= 3336  where dbid= 1125019152 ;
call del_JUR_from_summary(1125019152);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2943 , WallDuration= 7205  where dbid= 1125231561 ;
call del_JUR_from_summary(1125231561);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2806 , WallDuration= 7083  where dbid= 1125233570 ;
call del_JUR_from_summary(1125233570);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 4767 , WallDuration= 6862  where dbid= 1125286549 ;
call del_JUR_from_summary(1125286549);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 1342 , WallDuration= 3549  where dbid= 1125322705 ;
call del_JUR_from_summary(1125322705);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2101 , WallDuration= 4005  where dbid= 1125341162 ;
call del_JUR_from_summary(1125341162);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 2634 , WallDuration= 4734  where dbid= 1125358480 ;
call del_JUR_from_summary(1125358480);
--
update JobUsageRecord set CpuSystemDuration= 2147483648 , CpuUserDuration= 4293 , WallDuration= 7014  where dbid= 1125369163 ;
call del_JUR_from_summary(1125369163);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 846 , WallDuration= 1652  where dbid= 1132100341 ;
call del_JUR_from_summary(1132100341);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 1247 , WallDuration= 2221  where dbid= 1132156411 ;
call del_JUR_from_summary(1132156411);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 590 , WallDuration= 4643  where dbid= 1132166891 ;
call del_JUR_from_summary(1132166891);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 545 , WallDuration= 2624  where dbid= 1132221801 ;
call del_JUR_from_summary(1132221801);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 1480 , WallDuration= 6238  where dbid= 1132247143 ;
call del_JUR_from_summary(1132247143);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 522 , WallDuration= 4865  where dbid= 1132248505 ;
call del_JUR_from_summary(1132248505);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 523 , WallDuration= 2482  where dbid= 1132292429 ;
call del_JUR_from_summary(1132292429);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 877 , WallDuration= 11560  where dbid= 1132302216 ;
call del_JUR_from_summary(1132302216);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 1330 , WallDuration= 3138  where dbid= 1132374305 ;
call del_JUR_from_summary(1132374305);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 7570 , WallDuration= 13150  where dbid= 1132398685 ;
call del_JUR_from_summary(1132398685);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 540 , WallDuration= 5460  where dbid= 1132440112 ;
call del_JUR_from_summary(1132440112);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 873 , WallDuration= 1573  where dbid= 1132532512 ;
call del_JUR_from_summary(1132532512);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 151 , WallDuration= 1460  where dbid= 1132578462 ;
call del_JUR_from_summary(1132578462);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 11168 , WallDuration= 14304  where dbid= 1132578519 ;
call del_JUR_from_summary(1132578519);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 11050 , WallDuration= 13635  where dbid= 1132596066 ;
call del_JUR_from_summary(1132596066);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 846 , WallDuration= 2124  where dbid= 1132611825 ;
call del_JUR_from_summary(1132611825);
--
update JobUsageRecord set CpuSystemDuration= 2147483647 , CpuUserDuration= 11781 , WallDuration= 15711  where dbid= 1132614153 ;
call del_JUR_from_summary(1132614153);
--
update JobUsageRecord set CpuSystemDuration= 18446744145 , CpuUserDuration= 96 , WallDuration= 544  where dbid= 1134313965 ;
call del_JUR_from_summary(1134313965);
--
update JobUsageRecord set CpuSystemDuration= 18446744128 , CpuUserDuration= 250 , WallDuration= 513  where dbid= 1135298292 ;
call del_JUR_from_summary(1135298292);
--
update JobUsageRecord set CpuSystemDuration= 18446744345 , CpuUserDuration= 1901 , WallDuration= 3980  where dbid= 1136436367 ;
call del_JUR_from_summary(1136436367);
--
update JobUsageRecord set CpuSystemDuration= 18446744323 , CpuUserDuration= 500 , WallDuration= 2774  where dbid= 1136856424 ;
call del_JUR_from_summary(1136856424);
--
update JobUsageRecord set CpuSystemDuration= 18446744438 , CpuUserDuration= 11197 , WallDuration= 14129  where dbid= 1136936442 ;
call del_JUR_from_summary(1136936442);
--
update JobUsageRecord set CpuSystemDuration= 18446744317 , CpuUserDuration= 213 , WallDuration= 1976  where dbid= 1137013852 ;
call del_JUR_from_summary(1137013852);
--
update JobUsageRecord set CpuSystemDuration= 18446744141 , CpuUserDuration= 434 , WallDuration= 1114  where dbid= 1137207132 ;
call del_JUR_from_summary(1137207132);
--
update JobUsageRecord set CpuSystemDuration= 18446744366 , CpuUserDuration= 4827 , WallDuration= 7551  where dbid= 1137392767 ;
call del_JUR_from_summary(1137392767);
--
update JobUsageRecord set CpuSystemDuration= 18446744341 , CpuUserDuration= 1556 , WallDuration= 3431  where dbid= 1137673183 ;
call del_JUR_from_summary(1137673183);
--
update JobUsageRecord set CpuSystemDuration= 18446744355 , CpuUserDuration= 2764 , WallDuration= 4869  where dbid= 1137722961 ;
call del_JUR_from_summary(1137722961);
--
update JobUsageRecord set CpuSystemDuration= 18446744267 , CpuUserDuration= 132 , WallDuration= 1567  where dbid= 1137742534 ;
call del_JUR_from_summary(1137742534);
--
update JobUsageRecord set CpuSystemDuration= 18446744238 , CpuUserDuration= 106 , WallDuration= 1595  where dbid= 1137745906 ;
call del_JUR_from_summary(1137745906);
--
update JobUsageRecord set CpuSystemDuration= 18446744139 , CpuUserDuration= 247 , WallDuration= 838  where dbid= 1137765379 ;
call del_JUR_from_summary(1137765379);
--
update JobUsageRecord set CpuSystemDuration= 18446744408 , CpuUserDuration= 7573 , WallDuration= 9921  where dbid= 1137769487 ;
call del_JUR_from_summary(1137769487);
--
update JobUsageRecord set CpuSystemDuration= 18446744481 , CpuUserDuration= 9031 , WallDuration= 12122  where dbid= 1137770264 ;
call del_JUR_from_summary(1137770264);
--
update JobUsageRecord set CpuSystemDuration= 18446744293 , CpuUserDuration= 440 , WallDuration= 2272  where dbid= 1137772845 ;
call del_JUR_from_summary(1137772845);
--
update JobUsageRecord set CpuSystemDuration= 18446744315 , CpuUserDuration= 587 , WallDuration= 2194  where dbid= 1137773108 ;
call del_JUR_from_summary(1137773108);
--
update JobUsageRecord set CpuSystemDuration= 18446744143 , CpuUserDuration= 1098 , WallDuration= 1960  where dbid= 1137792800 ;
call del_JUR_from_summary(1137792800);
--
update JobUsageRecord set CpuSystemDuration= 18446744306 , CpuUserDuration= 1019 , WallDuration= 3026  where dbid= 1137812389 ;
call del_JUR_from_summary(1137812389);
--
update JobUsageRecord set CpuSystemDuration= 18446744161 , CpuUserDuration= 1162 , WallDuration= 2641  where dbid= 1137818382 ;
call del_JUR_from_summary(1137818382);
--
update JobUsageRecord set CpuSystemDuration= 18446744456 , CpuUserDuration= 9831 , WallDuration= 12652  where dbid= 1137841436 ;
call del_JUR_from_summary(1137841436);
--
update JobUsageRecord set CpuSystemDuration= 18446744157 , CpuUserDuration= 667 , WallDuration= 1525  where dbid= 1137843100 ;
call del_JUR_from_summary(1137843100);
--
update JobUsageRecord set CpuSystemDuration= 18446744446 , CpuUserDuration= 12179 , WallDuration= 14947  where dbid= 1137860445 ;
call del_JUR_from_summary(1137860445);
--
update JobUsageRecord set CpuSystemDuration= 18446744339 , CpuUserDuration= 798 , WallDuration= 2777  where dbid= 1137860448 ;
call del_JUR_from_summary(1137860448);
--
update JobUsageRecord set CpuSystemDuration= 18446744371 , CpuUserDuration= 4262 , WallDuration= 7855  where dbid= 1137957607 ;
call del_JUR_from_summary(1137957607);
--
update JobUsageRecord set CpuSystemDuration= 18446744165 , CpuUserDuration= 937 , WallDuration= 1975  where dbid= 1137959579 ;
call del_JUR_from_summary(1137959579);
--
update JobUsageRecord set CpuSystemDuration= 18446744336 , CpuUserDuration= 763 , WallDuration= 5187  where dbid= 1138006032 ;
call del_JUR_from_summary(1138006032);
--
update JobUsageRecord set CpuSystemDuration= 18446744160 , CpuUserDuration= 978 , WallDuration= 2072  where dbid= 1138075324 ;
call del_JUR_from_summary(1138075324);
--
update JobUsageRecord set CpuSystemDuration= 18446744480 , CpuUserDuration= 10853 , WallDuration= 14991  where dbid= 1138075868 ;
call del_JUR_from_summary(1138075868);
--
update JobUsageRecord set CpuSystemDuration= 18446744174 , CpuUserDuration= 972 , WallDuration= 2127  where dbid= 1138082573 ;
call del_JUR_from_summary(1138082573);
--
update JobUsageRecord set CpuSystemDuration= 18446744280 , CpuUserDuration= 146 , WallDuration= 3461  where dbid= 1138102217 ;
call del_JUR_from_summary(1138102217);
--
update JobUsageRecord set CpuSystemDuration= 18446744292 , CpuUserDuration= 161 , WallDuration= 3218  where dbid= 1138103714 ;
call del_JUR_from_summary(1138103714);
--
update JobUsageRecord set CpuSystemDuration= 18446744435 , CpuUserDuration= 13508 , WallDuration= 15972  where dbid= 1138259389 ;
call del_JUR_from_summary(1138259389);
--
update JobUsageRecord set CpuSystemDuration= 18446744158 , CpuUserDuration= 682 , WallDuration= 1516  where dbid= 1138316249 ;
call del_JUR_from_summary(1138316249);
--
update JobUsageRecord set CpuSystemDuration= 18446744332 , CpuUserDuration= 1841 , WallDuration= 3942  where dbid= 1138365491 ;
call del_JUR_from_summary(1138365491);
--
update JobUsageRecord set CpuSystemDuration= 18446744467 , CpuUserDuration= 14616 , WallDuration= 17223  where dbid= 1138501496 ;
call del_JUR_from_summary(1138501496);
--
update JobUsageRecord set CpuSystemDuration= 18446744303 , CpuUserDuration= 379 , WallDuration= 4261  where dbid= 1138538303 ;
call del_JUR_from_summary(1138538303);
--
update JobUsageRecord set CpuSystemDuration= 18446744319 , CpuUserDuration= 571 , WallDuration= 2802  where dbid= 1138539104 ;
call del_JUR_from_summary(1138539104);
--
update JobUsageRecord set CpuSystemDuration= 18446744331 , CpuUserDuration= 725 , WallDuration= 2629  where dbid= 1138540994 ;
call del_JUR_from_summary(1138540994);
--
update JobUsageRecord set CpuSystemDuration= 18446744164 , CpuUserDuration= 939 , WallDuration= 1958  where dbid= 1138597145 ;
call del_JUR_from_summary(1138597145);
--
update JobUsageRecord set CpuSystemDuration= 18446744440 , CpuUserDuration= 11246 , WallDuration= 14365  where dbid= 1138654997 ;
call del_JUR_from_summary(1138654997);
--
update JobUsageRecord set CpuSystemDuration= 18446744373 , CpuUserDuration= 4640 , WallDuration= 7716  where dbid= 1138727477 ;
call del_JUR_from_summary(1138727477);
--
update JobUsageRecord set CpuSystemDuration= 18446744150 , CpuUserDuration= 820 , WallDuration= 1528  where dbid= 1138816407 ;
call del_JUR_from_summary(1138816407);
--
update JobUsageRecord set CpuSystemDuration= 18446744570 , CpuUserDuration= 28072 , WallDuration= 28556  where dbid= 1139342299 ;
call del_JUR_from_summary(1139342299);
