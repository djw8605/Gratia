set autocommit=0;
DELETE FROM OSG_DATA WHERE Month = 02 AND Year = 2007 ;
INSERT INTO OSG_DATA VALUES ('SPRACE', 'cms', '1732', '6833', '11451', '9126', '15295', '02', '2007', '2007-02-01', '2007-02-28', '1.676', '2007-10-06 09:21:56');
INSERT INTO OSG_DATA VALUES ('UTA_SWT2', 'usatlas', '51636', '142838', '220398', '144161', '222440', '02', '2007', '2007-02-01', '2007-02-28', '1.543', '2007-10-06 09:21:56');
INSERT INTO OSG_DATA VALUES ('MWT2_IU', 'usatlas', '114943', '54458', '97317', '57083', '102007', '02', '2007', '2007-02-01', '2007-02-28', '1.787', '2007-10-06 09:21:56');
INSERT INTO OSG_DATA VALUES ('UCSDT2', 'cms', '8498', '58774', '50017', '76481', '65085', '02', '2007', '2007-02-01', '2007-02-28', '0.851000', '2007-10-06 09:21:57');
INSERT INTO OSG_DATA VALUES ('UCSDT2', 'usatlas', '1197', '83', '71', '87', '74', '02', '2007', '2007-02-03', '2007-02-26', '0.851000', '2007-10-06 09:21:57');
INSERT INTO OSG_DATA VALUES ('MIT_CMS', 'cms', '9150', '37581', '29689', '164360', '129844', '02', '2007', '2007-02-01', '2007-02-28', '0.79', '2007-10-06 09:21:57');
INSERT INTO OSG_DATA VALUES ('MIT_CMS', 'usatlas', '161', '0', '0', '0', '0', '02', '2007', '2007-02-01', '2007-02-28', '0.79', '2007-10-06 09:21:57');
INSERT INTO OSG_DATA VALUES ('UFlorida-PG', 'cms', '23877', '35050', '56149', '38906', '62327', '02', '2007', '2007-02-01', '2007-02-28', '1.602', '2007-10-06 09:21:57');
INSERT INTO OSG_DATA VALUES ('UFlorida-PG', 'usatlas', '2519', '151', '242', '161', '258', '02', '2007', '2007-02-01', '2007-02-28', '1.602', '2007-10-06 09:21:57');
INSERT INTO OSG_DATA VALUES ('UTA_DPCC', 'cms', '12', '0', '0', '0', '0', '02', '2007', '2007-02-09', '2007-02-28', '0.851000', '2007-10-06 09:21:58');
INSERT INTO OSG_DATA VALUES ('UTA_DPCC', 'usatlas', '147090', '107908', '91829', '111675', '95035', '02', '2007', '2007-02-01', '2007-02-28', '0.851000', '2007-10-06 09:21:58');
INSERT INTO OSG_DATA VALUES ('Nebraska', 'cms', '58364', '112850', '178077', '129955', '205068', '02', '2007', '2007-02-01', '2007-02-28', '1.578', '2007-10-06 09:21:58');
INSERT INTO OSG_DATA VALUES ('Nebraska', 'usatlas', '289', '0', '0', '3', '4', '02', '2007', '2007-02-01', '2007-02-28', '1.578', '2007-10-06 09:21:58');
INSERT INTO OSG_DATA VALUES ('GLOW', 'cms', '43680', '169223', '144008', '231227', '196774', '02', '2007', '2007-02-01', '2007-02-28', '0.851000', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('GLOW', 'usatlas', '116', '0', '0', '0', '0', '02', '2007', '2007-02-01', '2007-02-28', '0.851000', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('UC_Teraport', 'usatlas', '2456', '2791', '3940', '4242', '5989', '02', '2007', '2007-02-05', '2007-02-16', '1.412', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('OU_OSCER_ATLAS', 'usatlas', '145', '2', '2', '7', '11', '02', '2007', '2007-02-01', '2007-02-27', '1.543', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('Purdue-RCAC', 'cms', '198', '6', '5', '116', '99', '02', '2007', '2007-02-07', '2007-02-28', '0.851000', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('Purdue-RCAC', 'usatlas', '58', '0', '0', '10', '8', '02', '2007', '2007-02-16', '2007-02-28', '0.851000', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('MWT2_UC', 'usatlas', '68971', '17959', '32093', '22936', '40987', '02', '2007', '2007-02-01', '2007-02-28', '1.787', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('USCMS-FNAL-WC1-CE', 'cms', '239866', '524844', '446642', '573638', '488166', '02', '2007', '2007-02-01', '2007-02-28', '0.851000', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('USCMS-FNAL-WC1-CE', 'usatlas', '2907', '140', '119', '439', '374', '02', '2007', '2007-02-01', '2007-02-28', '0.851000', '2007-10-06 09:21:59');
INSERT INTO OSG_DATA VALUES ('Purdue-Lear', 'cms', '6405', '25918', '22056', '30291', '25777', '02', '2007', '2007-02-01', '2007-02-28', '0.851000', '2007-10-06 09:22:00');
INSERT INTO OSG_DATA VALUES ('CIT_CMS_T2', 'cms', '13464', '12102', '10299', '13378', '11385', '02', '2007', '2007-02-01', '2007-02-28', '0.851000', '2007-10-06 09:22:00');
commit;
set autocommit=0;
UPDATE OSG_DATA SET  NJobs=Njobs+44058,  SumCPU=SumCPU+106089,  NormSumCPU=NormSumCPU+163695,  SumWCT=SumWCT+107223,  NormSumWCT=NormSumWCT+165445,  Month=2,  YEAR=2007,  RecordStart='2007-02-01',  RecordEnd='2007-02-28',  NormFactor=1.543, MeasurementDate = '2007-10-06 09:35:55'   WHERE ExecutingSite = 'UTA_SWT2'    AND LCGUserVO = 'usatlas'    AND Month = 02    AND Year  = 2007 ;
UPDATE OSG_DATA SET  NJobs=Njobs+11,  SumCPU=SumCPU+0,  NormSumCPU=NormSumCPU+0,  SumWCT=SumWCT+3,  NormSumWCT=NormSumWCT+2,  Month=2,  YEAR=2007,  RecordStart='2007-02-07',  RecordEnd='2007-02-16',  NormFactor=0.851000, MeasurementDate = '2007-10-06 09:40:08'   WHERE ExecutingSite = 'Purdue-RCAC'    AND LCGUserVO = 'usatlas'    AND Month = 02    AND Year  = 2007 ;
UPDATE OSG_DATA SET  NJobs=Njobs+7670,  SumCPU=SumCPU+1674,  NormSumCPU=NormSumCPU+2991,  SumWCT=SumWCT+2213,  NormSumWCT=NormSumWCT+3955,  Month=2,  YEAR=2007,  RecordStart='2007-02-01',  RecordEnd='2007-02-28',  NormFactor=1.787, MeasurementDate = '2007-10-06 09:40:25'   WHERE ExecutingSite = 'MWT2_UC'    AND LCGUserVO = 'usatlas'    AND Month = 02    AND Year  = 2007 ;
commit;
